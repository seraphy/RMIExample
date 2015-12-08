package jp.seraphyware.rmiexample;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jp.seraphyware.rmiexample.rmi.Downloader;
import jp.seraphyware.rmiexample.rmi.RemoteFileIO;
import jp.seraphyware.rmiexample.rmi.RemoteObjectHelper;
import jp.seraphyware.rmiexample.rmi.Uploader;

/**
 * サーバーオブジェクトの実装.
 */
public class RemoteFileIOImpl implements RemoteFileIO, Unreferenced {

	@Override
	public List<String> getFiles() throws RemoteException, IOException {
		// あえとFile#listFiles()を使用する.
		// Files#list() はハンドル解放に漏れがあるように見受けられるので使わない (java8u60)
		File[] files = getBaseDir().listFiles();
		if (files == null) {
			throw new IOException("can't access folder.");
		}
		return Arrays.stream(files).filter(file -> file.isFile())
				.map(file -> file.getName()).sorted()
				.collect(Collectors.toList());
	}

	private static class UploaderImpl<T extends OutputStream> implements Uploader, Unreferenced {

		private final Logger logger = Logger.getLogger(getClass().getName());

		private T recv;

		private CompletableFuture<T> future;

		private Throwable ex;

		private boolean closed;

		public UploaderImpl(T recv, CompletableFuture<T> future) {
			Objects.requireNonNull(recv);
			this.recv = recv;
			this.future = future;
		}

		@Override
		public void write(byte[] data) throws IOException {
			if (data != null && data.length > 0) {
				logger.info("receive data: len=" + data.length);
				recv.write(data);
			}
		}

		@Override
		public void cancel(Throwable ex) throws RemoteException {
			this.ex = ex;
		}

		@Override
		public void close() throws IOException {
			if (!closed) {
				logger.info("◆◆CLOSED◆◆" + this);
				recv.close();
				closed = true;

				if (ex != null) {
					future.completeExceptionally(ex);
				} else {
					future.complete(recv);
				}
			}
		}

		@Override
		public void unreferenced() {
			// リモート側よりcloseが呼び出されると、それをトリガーとして
			// unexportを呼び出すので、結果的にクライアントからのunreferencedは
			// 呼び出されなくなる.
			// (クライアント側の参照が放置されて分散GCが働いた場合には効果あり.)
			logger.info("◆◆UNREFERENCED◆◆" + this);
			try {
				close();

			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	@Override
	public Uploader upload(String name) throws RemoteException, IOException {
		showMessage("upload: name=" + name);
		if (name == null || name.contains("\\") || name.contains("/")) {
			throw new IOException("illegal file name.");
		}

		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

		File outFile = new File(getBaseDir(), name);

		OutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));
		showMessage("file open. " + outFile);

		CompletableFuture<OutputStream> future = new CompletableFuture<>();
		Uploader uploader = new UploaderImpl<>(bos, future);

		future.whenComplete((ret, ex) -> {
			try {
				bos.close();
				showMessage("file closed.");

			} catch (Exception ex2) {
				if (ex == null) {
					ex = ex2;
				}
			}

			if (ex != null) {
				showMessage(ex.toString());
			}

			try {
				helper.unexportObject(uploader, true);
			} catch (RemoteException ex2) {
				throw new RuntimeException(ex2);
			}
		});

		return helper.exportObject(uploader);
	}

	private static class DownloaderImpl<T extends InputStream>
			implements Downloader, Unreferenced {

		private final Logger logger = Logger.getLogger(getClass().getName());

		private T send;

		private CompletableFuture<T> future;

		private Throwable ex;

		private boolean closed;

		public DownloaderImpl(T send, CompletableFuture<T> future) {
			Objects.requireNonNull(send);
			this.send = send;
			this.future = future;
		}

		@Override
		public long fileSize() throws RemoteException, IOException {
			return -1;
		}

		@Override
		public long lastModified() throws RemoteException, IOException {
			return -1;
		}

		@Override
		public byte[] read(int bufsiz) throws RemoteException, IOException {
			byte[] buf = new byte[bufsiz];
			int len = send.read(buf);
			if (len < 0) {
				// EOFに達している場合
				return null;
			}

			// バッファサイズと等しい場合は、そのまま返す
			if (len == buf.length) {
				return buf;
			}

			// 実際に読み込めたサイズがバッファサイズより小さければ
			// 実際のサイズに合わせた配列に詰め替えて返す.
			// ※ TCP/IPに不要なバイトを流すほうが、
			// 配列を作り直すよりコストが高いと思われるため。
			byte[] cpy = new byte[len];
			System.arraycopy(buf, 0, cpy, 0, len);
			return cpy;
		}

		@Override
		public void cancel(Throwable ex) throws RemoteException {
			this.ex = ex;
		}

		@Override
		public void close() throws IOException {
			if (!closed) {
				logger.info("◆◆CLOSED◆◆" + this);
				send.close();
				closed = true;

				if (ex != null) {
					future.completeExceptionally(ex);
				} else {
					future.complete(send);
				}
			}
		}

		@Override
		public void unreferenced() {
			// リモート側よりcloseが呼び出されると、それをトリガーとして
			// unexportを呼び出すので、結果的にクライアントからのunreferencedは
			// 呼び出されなくなる.
			// (クライアント側の参照が放置されて分散GCが働いた場合には効果あり.)
			logger.info("◆◆UNREFERENCED◆◆" + this);
			try {
				close();

			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	@Override
	public Downloader download(String name)
			throws RemoteException, IOException {
		showMessage("download: name=" + name);

		File inpFile = new File(getBaseDir(), name);

		InputStream is = new BufferedInputStream(new FileInputStream(inpFile));
		showMessage("file open. " + inpFile);

		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

		CompletableFuture<InputStream> future = new CompletableFuture<>();
		Downloader downloader = new DownloaderImpl<InputStream>(is, future) {

			@Override
			public long lastModified() throws RemoteException, IOException {
				return inpFile.lastModified();
			}

			@Override
			public long fileSize() throws RemoteException, IOException {
				return inpFile.length();
			}
		};

		future.whenComplete((ret, ex) -> {
			try {
				is.close();
				showMessage("file closed.");

			} catch (Exception ex2) {
				if (ex == null) {
					ex = ex2;
				}
			}

			if (ex != null) {
				showMessage("download aborted." + ex.toString());

			} else {
				showMessage("download complete.");
			}

			try {
				helper.unexportObject(downloader, true);
			} catch (RemoteException ex2) {
				throw new RuntimeException(ex2);
			}
		});

		return helper.exportObject(downloader);
	}

	@Override
	public void unreferenced() {
		// 参照数が0になるたびに呼び出される.
		// (クライアント側で参照を放置し分散GCでゼロになるたびに呼び出される.)
		showMessage("★★UNREFERENCED: " + this + "/isProxyClass="
				+ Proxy.isProxyClass(this.getClass()));
	}

	protected File getBaseDir() {
		return new File(".");
	}

	protected void showError(Throwable ex) {
		ex.printStackTrace(System.out);
	}

	protected void showMessage(String message) {
		System.out.println(message);
	}
}
