package jp.seraphyware.rmiexample.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * サーバオブジェクト用ファイル入出力インターフェイス
 */
public interface RemoteFileIO extends Remote {

	/**
	 * ファイル一覧を返す.
	 * @return ファイルの一覧
	 * @throws RemoteException
	 * @throws IOException
	 */
	List<String> getFiles() throws RemoteException, IOException;

	/**
	 * アップロードする.
	 * @param name ファイル名
	 * @return アップローダ
	 * @throws RemoteException
	 * @throws IOException
	 */
	Uploader upload(String name) throws RemoteException, IOException;

	/**
	 * ダウンロードする.
	 * @param name ファイル名
	 * @return ダウンローダ
	 * @throws RemoteException
	 * @throws IOException
	 */
	Downloader download(String name) throws RemoteException, IOException;
}
