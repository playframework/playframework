/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;

import java.util.Date;

public class Birthday {

    @play.data.format.Formats.DateTime(pattern = "customFormats.date")
    private Date date;

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}