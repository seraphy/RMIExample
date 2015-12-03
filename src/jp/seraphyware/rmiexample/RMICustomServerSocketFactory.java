package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.logging.Logger;

public class RMICustomServerSocketFactory implements RMIServerSocketFactory {

	private static final Logger log = Logger
			.getLogger(RMICustomServerSocketFactory.class.getName());

	@FunctionalInterface
	public interface ServerSocketListener {

		void notifyServerSocket(RMICustomServerSocketFactory factory,
				ServerSocket socket, boolean create);
	}

	private static ServerSocketListener serverSocketListener;

	public static ServerSocketListener getServerSocketListener() {
		return serverSocketListener;
	}

	public static void setServerSocketListener(ServerSocketListener l) {
		serverSocketListener = l;
	}

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
		ServerSocket socket = new ServerSocket(port) {
			@Override
			public synchronized void close() throws IOException {
				removeSocket(this);
				super.close();
			}
		};
		addSocket(socket);
		return socket;
	}

	protected void addSocket(ServerSocket socket) {
		log.info("★★CREATE SERVER SOCKET: " + socket.getLocalSocketAddress()
				+ ":" + socket.getLocalPort());
		if (serverSocketListener != null) {
			serverSocketListener.notifyServerSocket(this, socket, true);
		}
	}

	protected void removeSocket(ServerSocket socket) {
		log.info("★★CLOSE SERVER SOCKET: " + socket.getLocalSocketAddress()
				+ ":" + socket.getLocalPort());
		if (serverSocketListener != null) {
			serverSocketListener.notifyServerSocket(this, socket, false);
		}
	}
}
