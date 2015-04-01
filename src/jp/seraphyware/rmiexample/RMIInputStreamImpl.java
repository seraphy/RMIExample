package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Objects;
import java.util.logging.Logger;


/**
 * InputStreamのリモート越え対応版
 */
public class RMIInputStreamImpl extends UnicastRemoteObject implements RMIInputStream, Unreferenced {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private InputStream src;

	public RMIInputStreamImpl(InputStream src) throws RemoteException {
		Objects.requireNonNull(src);
		this.src = src;
		logger.info(() -> "exportObject: " + this);
	}

	@Override
	public byte[] read(int len) throws IOException {
		byte[] buf = new byte[len];
		int actualLen = src.read(buf, 0, len);
		if (actualLen <= 0) {
			// ファイル終端の場合
			return new byte[0];
		}

		if (actualLen == len) {
			// 要求サイズと等しい場合
			return buf;
		}

		// 実際のサイズの配列に直す.
		byte[] buf2 = new byte[actualLen];
		System.arraycopy(buf, 0, buf2, 0, actualLen);
		return buf2;
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
