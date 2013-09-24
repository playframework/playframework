/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server;

public interface ServerWithStop {

	public void stop();
	public java.net.InetSocketAddress mainAddress(); 

}