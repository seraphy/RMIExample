package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Downloader extends Remote, AutoCloseable {

	byte[] read() throws RemoteException, IOException;

	void cancel(Throwable ex) throws RemoteException;

	@Override
	void close() throws RemoteException, IOException;
}
