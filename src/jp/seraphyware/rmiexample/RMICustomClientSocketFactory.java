package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class RMICustomClientSocketFactory
		extends RMICustomSocketFactoryBase<Socket>
		implements RMIClientSocketFactory, Serializable {

	private static final long serialVersionUID = -4595294305398248285L;

	private static final Logger log = Logger
			.getLogger(RMICustomClientSocketFactory.class.getName());

	private UUID uuid;

	private static Consumer<RMICustomClientSocketFactory> clientSocketFactoryHandler;

	public static void setClientFactoryHandler(
			Consumer<RMICustomClientSocketFactory> clientSocketFactoryHandler) {
		RMICustomClientSocketFactory.clientSocketFactoryHandler = clientSocketFactoryHandler;
	}

	public Consumer<RMICustomClientSocketFactory> getClientFactoryHandler() {
		return clientSocketFactoryHandler;
	}

	public RMICustomClientSocketFactory() {
		log.info("★★RMICustomClientSocketFactory#ctor()");
		if (clientSocketFactoryHandler != null) {
			clientSocketFactoryHandler.accept(this);
		}
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (clientSocketFactoryHandler != null) {
			clientSocketFactoryHandler.accept(this);
		}
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public int hashCode() {
		return uuid == null ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RMICustomClientSocketFactory) {
			RMICustomClientSocketFactory o = (RMICustomClientSocketFactory) obj;
			return (uuid == null) ? (o.uuid == null) : uuid.equals(o.uuid);
		}
		return false;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		log.info("★★CREATE CLIENT SOCKET(" + uuid + "): " + host + ":" + port);
		Socket socket = new Socket(host, port) {
			@Override
			public synchronized void close() throws IOException {
				super.close();

				log.info("★★CLOSE CLIENT SOCKET(" + uuid + "): " + host + ":"
						+ port);
				removeSocket(this);
			}
		};
		addSocket(socket);
		return socket;
	}
}
