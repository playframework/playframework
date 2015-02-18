/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api;

import java.io.*;
import java.util.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper for `PlayException`.
 */
public class PlayException extends UsefulException {

    private final AtomicLong generator = new AtomicLong(System.currentTimeMillis());
   
    /**
     * Generates a new unique exception ID.
     */
    private String nextId() { 
        return java.lang.Long.toString(generator.incrementAndGet(), 26);
    }
  
    public PlayException(String title, String description, Throwable cause) {
        super(title + "[" + description + "]",cause);
        this.title = title;
        this.description = description;
        this.id = nextId();
        this.cause = cause;
    }
    
    public PlayException(String title, String description) {
        super(title + "[" + description + "]");
        this.title = title;
        this.description = description;
        this.id = nextId();
        this.cause = null;
    }
  
    /**
     * Adds source attachment to a Play exception.
     */
    public static abstract class ExceptionSource extends PlayException {
  
        public ExceptionSource(String title, String description, Throwable cause) {
          super(title, description,cause);
        }
     
        public ExceptionSource(String title, String description) {
          super(title, description);
        }
  
        /**
         * Error line number, if defined.
         */
        public abstract Integer line();
  
        /**
         * Column position, if defined.
         */
        public abstract Integer position();
  
        /**
         * Input stream used to read the source content.
         */
        public abstract String input();
  
        /**
         * The source file name if defined.
         */
        public abstract String sourceName();
  
        /**
         * Extracts interesting lines to be displayed to the user.
         *
         * @param border number of lines to use as a border
         */
        public InterestingLines interestingLines(int border) {
            try {
                if(input() == null || line() == null) {
                    return null;
                }
                String[] lines = input().split("\n");
                int firstLine = Math.max(0, line() - border);
                int lastLine = Math.min(lines.length - 1, line() + border);
                List<String> focusOn = new ArrayList<String>();
                for(int i = firstLine; i <= lastLine; i++) {
                    focusOn.add(lines[i]);
                }
                return new InterestingLines(firstLine + 1, focusOn.toArray(new String[focusOn.size()]), line() - firstLine - 1);
            } catch(Throwable e) {
                e.printStackTrace();
                return null;
            }
        }

        public String toString() {
            return super.toString() + " in " + sourceName() + ":" + line();
        }
    }
  
    /**
     * Adds any attachment to a Play exception.
     */
    public static abstract class ExceptionAttachment extends PlayException {
  
        public ExceptionAttachment(String title, String description, Throwable cause) {
            super(title, description, cause);
        }
  
        public ExceptionAttachment(String title, String description) {
            super(title, description);
        }
     
        /**
         * Content title.
         */
        public abstract String subTitle(); 
  
        /**
         * Content to be displayed.
         */
        public abstract String content();
  
    }
  
    /**
     * Adds a rich HTML description to a Play exception.
     */
    public static abstract class RichDescription extends ExceptionAttachment {
     
        public RichDescription(String title, String description, Throwable cause) {
            super(title, description, cause);
        }
  
        public RichDescription(String title, String description) {
            super(title, description);
        }
     
        /**
         * The new description formatted as HTML.
         */
        public abstract String htmlDescription();
  
    }

    public static class InterestingLines {
  
        public final int firstLine;
        public final int errorLine;
        public final String[] focus;

        public InterestingLines(int firstLine, String[] focus, int errorLine){
            this.firstLine = firstLine;
            this.errorLine = errorLine;
            this.focus = focus;
        }

    }
  
}
