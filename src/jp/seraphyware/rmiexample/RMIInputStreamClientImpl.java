package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Logger;

public class RMIInputStreamClientImpl extends InputStream {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private RMIInputStream inp;

	private final int chunksize = 64 * 1024;

	private byte[] buf;

	private int pos;

	public RMIInputStreamClientImpl(RMIInputStream inp) {
		Objects.requireNonNull(inp);
		this.inp = inp;
	}

	@Override
	public int read() throws IOException {
		if (buf != null) {
			if (buf.length == 0) {
				// 終端済み
				return -1;
			}

			// バッファを進める
			int zan = buf.length - pos;
			if (zan > 0) {
				return buf[pos++];
			}
		}

		// バッファにロードする.
		buf = inp.read(chunksize);
		if (buf.length == 0) {
			// 終端の場合
			return -1;
		}

		// 最初のデータ
		pos = 1;
		return buf[0];
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (buf != null) {
			int zan = buf.length - pos;
			if (zan >= len) {
				// 必要なサイズがバッファに残っている場合
				System.arraycopy(buf, pos, b, off, len);
				pos += len;
				return len;

			} else if (zan > 0) {
				// 必要なサイズがバッファ以上の場合は
				// 現在のバッファをすべて出しておく
				System.arraycopy(buf, pos, b, off, zan);
			}

			// バッファのリセット
			buf = null;
			pos = 0;

			// 残りの分
			len -= zan;
			off += zan;
		}

		// 直接読み込み
		byte[] tmp = inp.read(len);
		System.arraycopy(tmp, 0, b, off, tmp.length);
		if (tmp.length == 0) {
			// 終端をマークする.
			buf = tmp;
			return -1;
		}
		return tmp.length;
	}

	@Override
	public void close() throws IOException {
		logger.info(() -> "close");
		buf = new byte[0];
		pos = 0;
		inp.close();
	}
}