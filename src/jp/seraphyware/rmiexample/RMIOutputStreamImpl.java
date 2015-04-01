package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Objects;
import java.util.logging.Logger;


public class RMIOutputStreamImpl extends UnicastRemoteObject implements
		RMIOutputStream, Unreferenced {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private OutputStream src;

	public RMIOutputStreamImpl(OutputStream src) throws RemoteException {
		Objects.requireNonNull(src);
		this.src = src;
		logger.info(() -> "exportObject: " + this);
	}

	@Override
	public void write(byte[] buf) throws IOException {
		src.write(buf, 0, buf.length);
	}

	@Override
	public void close() throws IOException {
		src.close();
		UnicastRemoteObject.unexportObject(this, false);
		logger.info(() -> "unexportObject: " + this);
	}

	@Override
	public void unreferenced() {
		logger.info(() -> "unreferenced: " + this);
		try {
			close();

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + System.identityHashCode(this)
				+ ":source=" + src;
	}
}
