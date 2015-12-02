package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.logging.Logger;

public class RMICustomServerSocketFactory extends
		RMICustomSocketFactoryBase<ServerSocket>implements RMIServerSocketFactory {

	private static final Logger log = Logger
			.getLogger(RMICustomServerSocketFactory.class.getName());

	public RMICustomServerSocketFactory() {
		log.info("★★RMICustomServerSocketFactory#ctor()");
	}

	@Override
	public int hashCode() {
		return RMICustomServerSocketFactory.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RMICustomServerSocketFactory) {
			return true;
		}
		return false;
	}

	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		log.info("★★CREATE SERVER SOCKET: " + port);
		ServerSocket socket = new ServerSocket(port) {
			@Override
			public synchronized void close() throws IOException {
				log.info("★★CLOSE SERVER SOCKET: " + port);
				super.close();
				removeSocket(this);
			}
		};
		addSocket(socket);
		return socket;
	}

}
