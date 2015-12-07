package jp.seraphyware.rmiexample.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Uploader extends Remote, AutoCloseable {

	void write(byte[] data) throws RemoteException, IOException;

	void cancel(Throwable ex) throws RemoteException;

	@Override
	void close() throws IOException, RemoteException;
}
