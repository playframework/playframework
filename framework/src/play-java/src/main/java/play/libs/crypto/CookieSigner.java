/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.crypto;

/**
 *
 */
public interface CookieSigner {

    public String sign(String message);

    public String signToken(String token);
}
