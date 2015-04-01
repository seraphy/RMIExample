package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.rmi.Remote;

/**
 * リモート対応の入力ストリーム
 */
public interface RMIInputStream extends AutoCloseable, Remote {

	/**
	 * データを取得する。
	 * @param len 長さ
	 * @return 取得できたデータ(lenと等しいか、それ未満)、末端に達した場合は空の配列
	 * @throws IOException
	 */
	byte[] read(int len) throws IOException;

	/**
	 * クローズする.
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException;
}
