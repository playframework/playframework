/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;

import java.util.Date;

public class Birthday {

    @play.data.format.Formats.DateTime(pattern = "customFormats.date")
    private Date date;

    // No annotation
    private Date alternativeDate;

    @play.data.format.Formats.DateTime // no args; use default pattern
    private Date otherDate;

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getAlternativeDate() {
        return this.alternativeDate;
    }

    public void setAlternativeDate(Date date) {
        this.alternativeDate = date;
    }

    public Date getOtherDate() {
        return this.otherDate;
    }

    public void setOtherDate(Date date) {
        this.otherDate = date;
    }
}