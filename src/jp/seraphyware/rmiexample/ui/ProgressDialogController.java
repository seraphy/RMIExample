package jp.seraphyware.rmiexample.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jp.seraphyware.rmiexample.util.XMLResourceBundleControl;

public class ProgressDialogController extends AbstractFXMLController implements Initializable {

	@FXML
	private Label message;

	@FXML
	private ProgressBar progress;

	@FXML
	private Button btnCancel;

	/**
	 * リソースバンドル
	 */
	private ResourceBundle resource;

	@Override
	protected Parent makeRoot() {
		// リソース
		resource = ResourceBundle.getBundle(
				ProgressDialogController.class.getName(), new XMLResourceBundleControl());

		// FXMLファイルとリソースバンドルより画面を構成する
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("ProgressDialog.fxml"));
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
		stg.initStyle(StageStyle.UTILITY);
		stg.initModality(Modality.WINDOW_MODAL);
		stg.setTitle(resource.getString("title"));
		stg.setOnCloseRequest(evt -> {
			// クローズ不可
			evt.consume();
		});
		stg.sizeToScene();
		return stg;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO 自動生成されたメソッド・スタブ

	}

    public final void setOnCancelAction(EventHandler<ActionEvent> value) {
    	btnCancel.setOnAction(value);
    }

    public final EventHandler<ActionEvent> getOnCancelAction() {
    	return btnCancel.getOnAction();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCancelActionProperty() {
    	return btnCancel.onActionProperty();
    }

	public void close() {
		if (hasStage()) {
			getStage().close();
		}
	}

	public void show() {
		getStage().show();
	}

	public StringProperty titleProperty() {
		return getStage().titleProperty();
	}

	public StringProperty messageProperty() {
		return message.textProperty();
	}

	public DoubleProperty progressProperty() {
		return progress.progressProperty();
	}
}
