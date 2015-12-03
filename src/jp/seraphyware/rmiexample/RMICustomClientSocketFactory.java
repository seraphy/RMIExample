package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class RMICustomClientSocketFactory
		implements RMIClientSocketFactory, Serializable {

	private static final long serialVersionUID = -4595294305398248285L;

	@FunctionalInterface
	public interface ClientSocketListener {

		void notifyClientSocket(RMICustomClientSocketFactory factory,
				Socket socket, boolean create);
	}

	private static final Logger log = Logger
			.getLogger(RMICustomClientSocketFactory.class.getName());

	private UUID uuid;

	public RMICustomClientSocketFactory() {
		log.info("★★RMICustomClientSocketFactory#ctor()");
	}

	private static ClientSocketListener clientSocketListener;

	public static ClientSocketListener getClientSocketListener() {
		return clientSocketListener;
	}

	public static void setClientSocketListener(ClientSocketListener l) {
		clientSocketListener = l;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RMICustomClientSocketFactory) {
			RMICustomClientSocketFactory o = (RMICustomClientSocketFactory) obj;
			return Objects.equals(uuid, o.uuid);
		}
		return false;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		UUID uuid = this.uuid;
		Socket socket = new Socket(host, port) {
			@Override
			public synchronized void close() throws IOException {
				removeSocket(this, uuid);
				super.close();
			}
		};
		addSocket(socket, uuid);
		return socket;
	}

	protected void addSocket(Socket socket, UUID uuid) {
		log.info("★★CREATE CLIENT SOCKET(" + uuid + "): "
				+ socket.getInetAddress() + ":"
				+ socket.getLocalSocketAddress());
		if (clientSocketListener != null) {
			clientSocketListener.notifyClientSocket(this, socket, true);
		}
	}

	protected void removeSocket(Socket socket, UUID uuid) {
		log.info("★★CLOSE CLIENT SOCKET(" + uuid + "): "
				+ socket.getInetAddress() + ":"
				+ socket.getLocalSocketAddress());
		if (clientSocketListener != null) {
			clientSocketListener.notifyClientSocket(this, socket, false);
		}
	}
}
