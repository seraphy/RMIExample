package jp.seraphyware.rmiexample;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * サーバーオブジェクトの実装.
 */
public class MyServerObjectImpl implements MyServerObject {

	@Override
	public void echo(Message message) throws RemoteException {
		// 接続元クライアントのホスト情報を文字列で取得する.
		String clientHost;
		try {
			clientHost = RemoteServer.getClientHost();

		} catch (ServerNotActiveException ex) {
			// リモートからでない場合は"not in remote call"となる.
			clientHost = "Unknown: " + ex.toString();
		}

		// メッセージを表示する.
		showMessage("clientHost=" + clientHost + "/message=" + message.toString());
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

		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CompletableFuture<ByteArrayOutputStream> future = new CompletableFuture<>();
		Uploader uploader = new UploaderImpl<>(bos, future);

		future.whenComplete((ret, ex) -> {
			if (ex != null) {
				showMessage(ex.toString());

			} else {
				byte[] data = ret.toByteArray();
				showMessage(new String(data));
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
		public byte[] read() throws RemoteException, IOException {
			byte[] buf = new byte[4096];
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
		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

		String msgs = IntStream.range(0, 100)
				.mapToObj(idx -> "Hello, World!" + idx)
				.collect(Collectors.joining("\r\n"));

		ByteArrayInputStream bis = new ByteArrayInputStream(msgs.getBytes());
		CompletableFuture<ByteArrayInputStream> future = new CompletableFuture<>();
		Downloader downloader = new DownloaderImpl<>(bis, future);

		future.whenComplete((ret, ex) -> {
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
	public String shutdown() throws RemoteException {
		return "do nothing";
	}

	protected void showError(Throwable ex) {
		ex.printStackTrace(System.out);
	}

	protected void showMessage(String message) {
		System.out.println(message);
	}
}
