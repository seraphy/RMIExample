package jp.seraphyware.rmiexample;

import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.logging.Logger;

public class RemoteObjectHelper {

	private static Logger log = Logger
			.getLogger(RemoteObjectHelper.class.getName());

	private static final RemoteObjectHelper inst = new RemoteObjectHelper();

	private RemoteObjectHelper() {
		init();
	}

	public static RemoteObjectHelper getInstance() {
		return inst;
	}

	/**
	 * サーバソケットファクトリ
	 */
	private final RMICustomServerSocketFactory serverSocketFactory = new RMICustomServerSocketFactory();

	/**
	 * ソケットファクトリ
	 */
	private final RMICustomClientSocketFactory clientSocketFactory = new RMICustomClientSocketFactory();

	/**
	 * ソケット数監視
	 */
	private final RMICustomSocketWatcher socketWatcher = new RMICustomSocketWatcher();

	private void init() {
		RMICustomServerSocketFactory.setServerSocketListener(socketWatcher);
		RMICustomClientSocketFactory.setClientSocketListener(socketWatcher);
	}

	public void updateClientSocketFactoryUUID() {
		UUID uuid = UUID.randomUUID();
		clientSocketFactory.setUUID(uuid);
		log.info("◆◆CLIENT SOCKET UUID=" + uuid);
	}

	public RMICustomSocketWatcher getSocketWatcher() {
		return socketWatcher;
	}

	public Registry createLocalRegistry(int registryPort)
			throws RemoteException {
		return LocateRegistry.createRegistry(registryPort,
				clientSocketFactory, serverSocketFactory);
	}

	public Registry getRegistry(String host, int port) throws RemoteException {
		return LocateRegistry.getRegistry(host, port, clientSocketFactory);
	}

	private int exportPort = 0;

	public int getExportPort() {
		return exportPort;
	}

	public void setExportPort(int exportPort) {
		this.exportPort = exportPort;
	}

	public <T extends Remote> T exportObject(T obj) throws RemoteException {
		@SuppressWarnings("unchecked")
		T stub = (T) UnicastRemoteObject.exportObject(obj, exportPort,
				clientSocketFactory, serverSocketFactory);
		if (Proxy.isProxyClass(stub.getClass()) &&
				Proxy.getInvocationHandler(
						stub) instanceof RemoteObjectInvocationHandler) {
			RemoteObject handler = (RemoteObject) Proxy
					.getInvocationHandler(stub);

			String refid = handler.getRef().remoteToString();
			log.info("▲▲▲exported refid=" + refid);
		}
		return stub;
	}

	public void unexportObject(Remote obj, boolean force) throws RemoteException {
		Remote stub = UnicastRemoteObject.toStub(obj);
		if (Proxy.isProxyClass(stub.getClass()) &&
				Proxy.getInvocationHandler(
						stub) instanceof RemoteObjectInvocationHandler) {
			RemoteObject handler = (RemoteObject) Proxy
					.getInvocationHandler(stub);

			String refid = handler.getRef().remoteToString();
			log.info("▼▼▼unexported refid=" + refid);
		}
		boolean result = UnicastRemoteObject.unexportObject(obj, force);
		log.info("▼▼▼▲unexported result=" + result);
	}
}
