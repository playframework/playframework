/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.data;

import play.libs.F.Option;
import play.data.validation.Constraints.ValidateWith;

public class MyBlueUser {
    public String name;

    @ValidateWith(BlueValidator.class)
    private String skinColor;
    
    @ValidateWith(value=BlueValidator.class, message="i-am-blue")
    private String hairColor;
    
    public String getSkinColor() {
        return skinColor;
    }
    
    public void setSkinColor(String value) {
        skinColor = value;
    }
    
    public String getHairColor() {
        return hairColor;
    }
    
    public void setHairColor(String value) {
        hairColor = value;
    }
}
