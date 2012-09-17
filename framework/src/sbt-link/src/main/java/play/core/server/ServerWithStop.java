package play.core.server;

public interface ServerWithStop {

	public void stop();
	public java.net.InetSocketAddress mainAddress(); 

}