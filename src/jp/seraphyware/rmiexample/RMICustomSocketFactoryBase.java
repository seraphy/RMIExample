package jp.seraphyware.rmiexample;

import java.util.LinkedList;
import java.util.function.Consumer;

public abstract class RMICustomSocketFactoryBase<S> {

	private LinkedList<S> sockets = new LinkedList<>();

	private Consumer<Integer> updateSocketCount;

	public void setUpdateSocketCount(Consumer<Integer> updateSocketCount) {
		this.updateSocketCount = updateSocketCount;
	}

	public Consumer<Integer> getUpdateSocketCount() {
		return updateSocketCount;
	}

	protected void addSocket(S socket) {
		int count;
		synchronized (sockets) {
			sockets.add(socket);
			count = sockets.size();
		}
		notifySocketCount(count);
	}

	protected void removeSocket(S socket) {
		int count;
		synchronized (sockets) {
			sockets.remove(socket);
			count = sockets.size();
		}
		notifySocketCount(count);
	}

	public int getNumOfSockets() {
		synchronized (sockets) {
			return sockets.size();
		}
	}

	protected void notifySocketCount(int count) {
		if (updateSocketCount != null) {
			updateSocketCount.accept(count);
		}
	}

	public void forceDisconnect(Consumer<S> closer) {
		LinkedList<S> cpy = new LinkedList<>(sockets);
		cpy.forEach(closer);
	}
}
