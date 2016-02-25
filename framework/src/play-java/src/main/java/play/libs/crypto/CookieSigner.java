/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.crypto;

/**
 *
 */
public interface CookieSigner {

    public String sign(String message);

    public String signToken(String token);
}
