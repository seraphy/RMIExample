package jp.seraphyware.rmiexample.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Downloader extends Remote, AutoCloseable {

	long lastModified() throws RemoteException, IOException;

	long fileSize() throws RemoteException, IOException;

	byte[] read(int bufsiz) throws RemoteException, IOException;

	void cancel(Throwable ex) throws RemoteException;

	@Override
	void close() throws RemoteException, IOException;
}
