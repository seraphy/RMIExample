package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.rmi.Remote;

public interface RMIOutputStream extends Remote, AutoCloseable {

	void write(byte[] buf) throws IOException;

	@Override
	public void close() throws IOException;
}
