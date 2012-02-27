package play.core.utils;

import java.util.*;

public class SQLSplitter implements Iterable<CharSequence> {

	/**
	 * Skips the index past the quote. 
	 *
	 * @param s The string
	 * @param start The starting character of the quote.
	 *
	 * @return The index that skips past the quote starting at start. If the quote does not start at that point, it simply returns start.
	 */
	static int consumeQuote(final CharSequence s, final int start) {
		if ( start >= s.length() ) return start;
		char ender;
		switch ( s.charAt(start) ) {
			case '\'':
				ender = '\'';
				break;
			case '"':
				ender = '"';
				break;
			case '[':
				ender = ']';
				break;

			case '`':
				ender = '`';
				break;
			case '$': {
				int quoteEnd = start + 1;
				for ( ; s.charAt(quoteEnd) != '$'; ++quoteEnd )
					if ( quoteEnd >= s.length() )
						return quoteEnd;
				int i = quoteEnd + 1;	
				while ( i < s.length() ) {
					if ( s.charAt(i) == '$' ) {
						boolean match = true;
						for ( int j = start; j <= quoteEnd && i < s.length(); ++j, ++i ) {
							if ( s.charAt(i) != s.charAt(j) ) {
								match = false;
								break;
							}
						}
						if ( match )
							return i;
					} else
						++i;
				}	
				return i;
			}

			default:
				return start;
		}

		boolean escaped = false;
		
		for ( int i = start + 1; i < s.length(); ++i ) {
			if ( escaped ) {
				escaped = false;
				continue;
			}
			char c = s.charAt(i);
			if ( c == '\\' )
				escaped = true;
			else if ( c == ender )
				return i + 1;
		}
		return s.length();
	}

	static boolean isNewLine(char c) {
		return c == '\n' || c == '\r';
	}
	
	/**
	 * Returns the index of the next line from a start location.
	 */
	static int consumeTillNextLine(final CharSequence s, int start) {
		while ( start < s.length() && !isNewLine(s.charAt(start)) )
			++start;
		while ( start < s.length() && isNewLine(s.charAt(start)) )
			++start;
		return start;
	}

	static boolean isNext(final CharSequence s, final int start, final char c) {
		if ( start + 1 < s.length() )
			return s.charAt(start + 1) == c;
		return false;
	}

	/**
	 * Skips the index past the comment. 
	 *
	 * @param s The string
	 * @param start The starting character of the comment
	 *
	 * @return The index that skips past the comment starting at start. If the comment does not start at that point, it simply returns start.
	 */
	static int consumeComment(final CharSequence s, int start) {
		if ( start >= s.length() ) return start;
		switch ( s.charAt(start) ) {
			case '-':
				if ( isNext(s, start, '-') )
					return consumeTillNextLine(s, start + 2);
				else
					return start;

			case '#':
				return consumeTillNextLine(s, start);

			case '/':
				if ( isNext(s, start, '*') ) {
					start += 2;
					while ( start < s.length() ) {
						if ( s.charAt(start) == '*' ) {
							++start;
							if ( start < s.length() && s.charAt(start) == '/' )
								return start + 1;
						} else
							++start;
					}
				}
				return start;

			case '{':
				while ( start < s.length() && s.charAt(start) != '}' )
					++start;
				return start + 1;

			default:
				return start;
		}
	}

	static int consumeParentheses(final CharSequence s, int start) {
		if ( start >= s.length() ) return start;
		switch ( s.charAt(start) ) {
			case '(':
				++start;
				while ( start < s.length() ) {
					if ( s.charAt(start) == ')' )
						return start + 1;
					start = nextChar(s, start);
				}
				break;
			default:
				break;
		}
		return start;
	}

	static int nextChar(final CharSequence sql, final int start) {
		int i = consumeParentheses(sql, consumeComment(sql, consumeQuote(sql, start)));
		if ( i == start ) return Math.min(start + 1, sql.length());
		do {
			final int j = consumeParentheses(sql, consumeComment(sql, consumeQuote(sql, i)));
			if ( j == i )
				return i;
			i = j;
		} while ( true );
	}

	/**
	 * Splits the SQL "properly" based on semicolons. Respecting quotes and comments.
	 */
	public static ArrayList<CharSequence> splitSQL(final CharSequence sql) {
		final ArrayList<CharSequence> ret = new ArrayList<CharSequence>();
		for ( CharSequence c : new SQLSplitter(sql) )
			ret.add(c);
		return ret;
	}

	final CharSequence sql;
	
	public SQLSplitter(final CharSequence sql) {
		this.sql = sql;
	}

	public Iterator<CharSequence> iterator() {
		return new Iterator<CharSequence>() {
			int i = 0, prev = 0;

			public boolean hasNext() {
				return prev < sql.length();
			}

			public CharSequence next() {
				while ( i < sql.length() ) {
					if ( sql.charAt(i) == ';' ) {
						++i;
						CharSequence ret = sql.subSequence(prev, i);
						prev = i;
						return ret;
					}
					i = nextChar(sql, i);
				}
				if ( prev != i ) {
					CharSequence ret = sql.subSequence(prev, i);
					prev = i;
					return ret;
				}
				throw new NoSuchElementException();
			}

			public void remove() { throw new UnsupportedOperationException(); }
		};
	}
}
