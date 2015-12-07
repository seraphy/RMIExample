package jp.seraphyware.rmiexample.rmi;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RMICustomSocketWatcher
		implements RMICustomClientSocketFactory.ClientSocketListener,
		RMICustomServerSocketFactory.ServerSocketListener {

	@FunctionalInterface
	public interface SocketNumberChangeListsner {

		void changeNumOfSockets(int serverSocket, int clientSocket);
	}

	private ConcurrentLinkedQueue<Socket> clientSockets = new ConcurrentLinkedQueue<>();

	private ConcurrentLinkedQueue<ServerSocket> serverSockets = new ConcurrentLinkedQueue<>();

	private SocketNumberChangeListsner numOfSocketsListener;

	public SocketNumberChangeListsner getNumOfSocketsListener() {
		return numOfSocketsListener;
	}

	public void setNumOfSocketsListener(
			SocketNumberChangeListsner numOfSocketsListener) {
		this.numOfSocketsListener = numOfSocketsListener;
	}

	@Override
	public void notifyClientSocket(RMICustomClientSocketFactory factory,
			Socket socket, boolean create) {
		if (create) {
			clientSockets.add(socket);
		} else {
			clientSockets.remove(socket);
		}
		notifyNumOfSockets();
	}

	@Override
	public void notifyServerSocket(RMICustomServerSocketFactory factory,
			ServerSocket socket, boolean create) {
		if (create) {
			serverSockets.add(socket);
		} else {
			serverSockets.remove(socket);
		}
		notifyNumOfSockets();
	}

	public int getNumOfClientSockets() {
		return clientSockets.size();
	}

	public int getNumOfServerSockets() {
		return serverSockets.size();
	}

	public void notifyNumOfSockets() {
		if (numOfSocketsListener != null) {
			numOfSocketsListener.changeNumOfSockets(
					getNumOfServerSockets(),
					getNumOfClientSockets()
					);
		}
	}
}
