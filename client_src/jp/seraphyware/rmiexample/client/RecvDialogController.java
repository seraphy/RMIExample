package jp.seraphyware.rmiexample.client;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.ui.AbstractFXMLController;
import jp.seraphyware.rmiexample.util.XMLResourceBundleControl;

public class RecvDialogController extends AbstractFXMLController
		implements Initializable {

	public static class Result {

		private String fileName;

		private File outFile;


		public String getFileName() {
			return fileName;
		}

		public File getOutFile() {
			return outFile;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public void setOutFile(File outFile) {
			this.outFile = outFile;
		}
	}

	@FXML
	private TextField txtOutputName;

	@FXML
	private ComboBox<String> cmbFiles;

	/**
	 * リソースバンドル
	 */
	private ResourceBundle resource;

	private ObservableList<String> files = FXCollections.observableArrayList();

	private Result result = null;

	@Override
	protected Parent makeRoot() {
		// リソース
		resource = ResourceBundle.getBundle(
				ClientMainController.class.getName(), new XMLResourceBundleControl());

		// FXMLファイルとリソースバンドルより画面を構成する
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("RecvDialog.fxml"));
		loader.setResources(resource);

		// コントローラは明示的に、このオブジェクトに関連づける
		loader.setController(this);

		// FXMLのロード
		Parent root;
		try {
			root = (Parent) loader.load();

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return root;
	}

	@Override
	protected Stage makeStage() {
		Stage stg = super.makeStage();
		stg.setTitle(resource.getString("title"));
		stg.initModality(Modality.WINDOW_MODAL);
		stg.setOnCloseRequest(evt -> onCancel());
		return stg;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		cmbFiles.setItems(files);
	}

	public void setFiles(List<String> files) {
		if (files == null) {
			this.files.clear();
		} else {
			this.files.setAll(files);
		}
	}

	public List<String> getFiles() {
		return new ArrayList<>(files);
	}

	private File lastUseDir;

	@FXML
	protected void onChooseOutputFile() {
		FileChooser fileChooser = new FileChooser();
		if (lastUseDir != null && lastUseDir.isDirectory()) {
			fileChooser.setInitialDirectory(lastUseDir);
		}
		File selectedFile = fileChooser.showSaveDialog(getStage());
		if (selectedFile != null) {
			txtOutputName.setText(selectedFile.toString());
		}
	}

	@FXML
	protected void onRecv() {
		result = new Result();
		result.setFileName(cmbFiles.getValue());
		result.setOutFile(new File(txtOutputName.getText()));
		getStage().close();
	}

	@FXML
	protected void onCancel() {
		result = null;
		getStage().close();
	}

	public Optional<Result> getResult() {
		return Optional.ofNullable(result);
	}

	public Optional<Result> showAndWait() {
		Stage stg = getStage();
		stg.showAndWait();
		return getResult();
	}
}
