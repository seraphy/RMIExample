package jp.seraphyware.rmiexample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class RMIOutputStreamClientImpl extends OutputStream {

	private RMIOutputStream ros;

	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	private final int bufsiz = 16 * 1024;

	public RMIOutputStreamClientImpl(RMIOutputStream ros) {
		Objects.requireNonNull(ros);
		this.ros = ros;
	}

	@Override
	public void write(int b) throws IOException {
		bos.write(b);
		if (bos.size() > bufsiz) {
			flush();
		}
	}


	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		bos.write(b, off, len);
		if (bos.size() > bufsiz) {
			flush();
		}
	}

	@Override
	public void flush() throws IOException {
		ros.write(bos.toByteArray());
		bos.reset();
	}

	@Override
	public void close() throws IOException {
		flush();
		ros.close();
	}
}
