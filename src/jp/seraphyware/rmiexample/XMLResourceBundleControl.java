package jp.seraphyware.rmiexample;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

public class XMLResourceBundleControl extends ResourceBundle.Control {
	/**
	 * 対応するフォーマット形式
	 */
	private static final String ext = "xml";

	/**
	 * 対応するフォーマットを返す.
	 */
	public List<String> getFormats(String baseName) {
		return Arrays.asList(ext);
	}

	/**
	 * 指定されたクラスローダから、名前、ロケール情報でリソースを選択し、 リソースバンドルを返します.
	 * http://docs.oracle.com/javase/6/docs/api/java/util/ResourceBundle.Control.html
	 */
	public ResourceBundle newBundle(String baseName, Locale locale,
			String format, ClassLoader loader, boolean reload)
			throws IllegalAccessException, InstantiationException, IOException {
		if (baseName == null || locale == null || format == null
				|| loader == null) {
			throw new NullPointerException();
		}

		ResourceBundle bundle = null;
		if (format.equals(ext)) {
			// xml形式の場合

			// ロケールと結合したリソース名を求める
			String bundleName = toBundleName(baseName, locale);

			// 対応するフォーマットと結合したリソース名を求める
			String resourceName = toResourceName(bundleName, format);

			// リソース名をクラスローダから取得する. (なければnull)
			InputStream stream = null;
			URL url = loader.getResource(resourceName);
			System.out.println("load resource bundle: " + resourceName + "="
					+ url); // ☆実験用☆

			if (url != null) {
				URLConnection connection = url.openConnection();
				if (connection != null) {
					if (reload) {
						// リロードの場合はキャッシュを無効にしてロードを試みる
						connection.setUseCaches(false);
					}
					stream = connection.getInputStream();
				}
			}

			// 取得されたリソースからXMLリソースバンドルとして読み込む
			if (stream != null) {
				try (BufferedInputStream bis = new BufferedInputStream(stream)) {
					bundle = new XMLResourceBundle(bis);
				}
			}
		}
		return bundle;
	}
}

class XMLResourceBundle extends ResourceBundle {

	/**
	 * XMLリソースバンドルのバックエンドとして、 Propertiesを使う.
	 */
	private Properties props = new Properties();

	XMLResourceBundle(InputStream is) throws IOException {
		props.loadFromXML(is);
	}

	@Override
	protected Object handleGetObject(String key) {
		return props.getProperty(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getKeys() {
		// Properties#keys()は、Objectを列挙するような型宣言だが
		// 実体はStringなので単にキャストすれば良い.
		// 互換性のない総称型同士をキャストすることはできないが、
		// 一旦、raw経由にすればキャスト可能になる。
		@SuppressWarnings("rawtypes")
		Enumeration enm = props.keys();
		return enm;
	}
}
