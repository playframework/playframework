package play.util;

import akka.util.ByteString;

import java.io.*;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @implNote    adapted, fixed and enhanced (refs https://tools.ietf.org/html/rfc7303 instead of http://www.ietf.org/rfc/rfc3023.txt)
 *              from https://rometools.github.io/rome/
 *
 */
public abstract class XmlEncodingDetective {

    private static final String US_ASCII = "US-ASCII";
    private static final String UTF_8 = "UTF-8";
    private static final String UTF_16BE = "UTF-16BE";
    private static final String UTF_16LE = "UTF-16LE";
    private static final String UTF_16 = "UTF-16";
    private static final String UTF_32LE = "UTF-32LE";
    private static final String UTF_32BE = "UTF-32BE";
    private static final String UTF_32 = "UTF-32";

    private static final int PROLOG_BUFFER_SIZE = 4096;

    private static final Pattern ENCODING_PATTERN = Pattern.compile("<\\?xml.*encoding[\\s]*=[\\s]*((?:\".[^\"]*\")|" +
            "(?:'.[^']*'))", Pattern.MULTILINE);

    private static final MessageFormat RAW_EX_1 = new MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML " +
            "prolog [{2}] encoding mismatch");
    private static final MessageFormat RAW_EX_2 = new MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML " +
            "prolog [{2}] unknown BOM");
    private static final MessageFormat HTTP_EX_1 = new MessageFormat(
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], BOM must be " +
                    "NULL");
    private static final MessageFormat HTTP_EX_2 = new MessageFormat(
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], encoding " +
                    "mismatch");

    /**
     * @param byteString
     * @param httpCharset
     * @param httpContentType
     * @return best effort encoding
     * @throws IOException when detecting fails
     */
    public static String detectEncoding(final ByteString byteString, final String httpCharset, final String
            httpContentType) throws IOException {

        final String bomEncoding = getBOMEncoding(byteString);
        int mark = 0;
        if (bomEncoding != null) {
            switch (bomEncoding) {
                case UTF_16BE:
                case UTF_16LE:
                    mark = 2;
                    break;
                case UTF_32BE:
                case UTF_32LE:
                    mark = 4;
                    break;
                default: // UTF-8
                    mark = 3;
            }
        }
        final String octetEncoding = getOctetEncoding(byteString, mark);
        if (octetEncoding != null) {
            if (bomEncoding != null && !octetEncoding.equals(bomEncoding)) {
                throw new RuntimeException("XML Encoding error: first octets pattern doesn't match existing BOM " +
                        "encoding");
            }
        }
        final String prologEncoding = getPrologEncoding(byteString, (bomEncoding == null) ? octetEncoding :
                bomEncoding, mark);
        String result = calculateHttpEncoding(httpContentType, httpCharset, bomEncoding, octetEncoding, prologEncoding);
        return result;
    }

    private static String getBOMEncoding(final ByteString byteString) {
        String encoding = null;
        Byte[] bytes = new Byte[4];
        byteString.copyToArray(bytes, 0, 4);

        if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
            encoding = UTF_16BE;
        } else if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
            if (bytes[2] == 0x00 && bytes[3] == 0x00) {
                encoding = UTF_32LE;
            } else {
                encoding = UTF_16LE;
            }
        } else if (bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
            encoding = UTF_8;
        } else if (bytes[0] == 0x00 && bytes[1] == 0x00 && bytes[2] == 0xFE && bytes[3] == 0xFF) {
            encoding = UTF_32BE;
        }
        return encoding;
    }

    private static String getOctetEncoding(final ByteString byteString, final int mark) {
        String encoding = null;
        Byte[] bytes = new Byte[4];
        byteString.copyToArray(bytes, mark, 4);
        if (bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F) {
            encoding = UTF_16BE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00) {
            encoding = UTF_16LE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x3F && bytes[2] == 0x78 && bytes[3] == 0x6D) {
            encoding = UTF_8;
        } else if (bytes[0] == 0x00 && bytes[1] == 0x00 && bytes[2] == 0x00 && bytes[3] == 0x3C) {
            encoding = UTF_32BE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x00 && bytes[3] == 0x00) {
            encoding = UTF_32LE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x78 && bytes[3] == 0x00) {
            encoding = UTF_32LE;
        }
        return encoding;
    }

    private static String getPrologEncoding(final ByteString byteString, final String fileEncoding, final int mark)
            throws IOException {
        String encoding = null;

        if (fileEncoding != null) {
            int bytesSize = byteString.size();
            int bufferSize = (bytesSize > PROLOG_BUFFER_SIZE) ? PROLOG_BUFFER_SIZE : bytesSize;
            Byte[] bytesAll = new Byte[bufferSize];
            byteString.copyToArray(bytesAll);
            Byte[] bytes = new Byte[bufferSize - mark];
            System.arraycopy(bytesAll,mark,bytes,0,bytes.length);
            byte[] bytesPrim = toPrimitive(bytes);
            final String firstChars = new String(bytesPrim, 0, bytes.length);
            final int firstGT = firstChars.indexOf(">");
            if (firstGT == -1) {
                throw new IOException("XML prolog or ROOT element not found on first " + bytesPrim.length + " bytes");
            }
            if (bytes.length > 0) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream(bytesPrim, 0, firstGT + 1),
                        fileEncoding);
                final BufferedReader bReader = new BufferedReader(reader);
                final StringBuffer prolog = new StringBuffer();
                String line = bReader.readLine();
                while (line != null) {
                    prolog.append(line);
                    line = bReader.readLine();
                }
                final Matcher m = ENCODING_PATTERN.matcher(prolog);
                if (m.find()) {
                    encoding = m.group(1).toUpperCase(Locale.ENGLISH);
                    encoding = encoding.substring(1, encoding.length() - 1);
                }
            }
        }

        return encoding;
    }

    private static String calculateHttpEncoding(final String httpContentType, final String httpCharset, final String
            bomEncoding, final String octetsEncoding, final String prologEncoding) throws IOException {
        String encoding;
        if (httpCharset == null) {
            encoding = calculateRawEncoding(bomEncoding, octetsEncoding, prologEncoding);
        } else if (bomEncoding != null && (httpCharset.equals(UTF_16BE) || httpCharset.equals(UTF_16LE))) {
            throw new IOException(HTTP_EX_1.format(new Object[]{httpContentType, httpCharset, bomEncoding,
                    octetsEncoding, prologEncoding}));
        } else if (httpCharset.equals(UTF_16) || httpCharset.equals(UTF_32)) {
            if (bomEncoding != null && bomEncoding.startsWith(httpCharset)) {
                encoding = bomEncoding;
            } else {
                throw new IOException(HTTP_EX_2.format(new Object[]{httpContentType, httpCharset, bomEncoding,
                        octetsEncoding, prologEncoding}));
            }
        } else {
            encoding = httpCharset;
        }

        return encoding;
    }

    private static String calculateRawEncoding(final String bomEncoding, final String octetsEncoding, final String
            prologEncoding) throws IOException {
        String encoding;
        if (bomEncoding == null) {
            if (octetsEncoding == null || prologEncoding == null) {
                encoding = UTF_8;
            } else if ((prologEncoding.equals(UTF_16) && (octetsEncoding.equals(UTF_16BE) || octetsEncoding.equals(UTF_16LE))) ||
                    (prologEncoding.equals(UTF_32) && (octetsEncoding.equals(UTF_32BE) || octetsEncoding.equals (UTF_32LE)))) {
                encoding = octetsEncoding;
            } else {
                encoding = prologEncoding;
            }
        } else if (bomEncoding.equals(UTF_8)) {
            if (octetsEncoding != null && !octetsEncoding.equals(UTF_8)) {
                throw new IOException(RAW_EX_1.format(new Object[]{bomEncoding, octetsEncoding, prologEncoding}));
            }
            if (prologEncoding != null && !prologEncoding.equals(UTF_8)) {
                throw new IOException(RAW_EX_1.format(new Object[]{bomEncoding, octetsEncoding, prologEncoding}));
            }
            encoding = UTF_8;
        } else if (bomEncoding.equals(UTF_16BE) || bomEncoding.equals(UTF_16LE) || bomEncoding.equals(UTF_32BE) ||
                bomEncoding.equals(UTF_32LE)) {
            if (octetsEncoding != null && !octetsEncoding.equals(bomEncoding)) {
                throw new IOException(RAW_EX_1.format(new Object[]{bomEncoding, octetsEncoding, prologEncoding}));
            }
            if (prologEncoding != null && !prologEncoding.equals(UTF_16) && !prologEncoding.equals(UTF_32) &&
                    !prologEncoding.equals(bomEncoding)) {
                throw new IOException(RAW_EX_1.format(new Object[]{bomEncoding, octetsEncoding, prologEncoding}));
            }
            encoding = bomEncoding;
        } else {
            throw new IOException(RAW_EX_2.format(new Object[]{bomEncoding, octetsEncoding, prologEncoding}));
        }
        return encoding;
    }

    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new byte[0];
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }
}
