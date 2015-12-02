package jp.seraphyware.rmiexample;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class AbstractFXMLController {

	private Window parent;

	private Stage stage;

	private Parent root;

	private Scene scene;

	public Window getParent() {
		return parent;
	}

	public void setParent(Window parent) {
		this.parent = parent;
	}

	public Stage getStage() {
		initStage();
		assert stage != null;
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	protected void initStage() {
		if (stage == null) {
			setStage(makeStage());
		}
	}

	protected Stage makeStage() {
		Stage stage = new Stage(StageStyle.DECORATED);
		stage.initOwner(parent);
		stage.initModality(Modality.NONE);

		// シーングラフの設定
		stage.setScene(getScene());

		return stage;
	}

	public Scene getScene() {
		initScene();
		assert scene != null;
		return scene;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	protected void initScene() {
		if (scene == null) {
			setScene(new Scene(getRoot()));
		}
	}

	public Parent getRoot() {
		initRoot();
		assert root != null;
		return root;
	}

	public void setRoot(Parent root) {
		this.root = root;
	}

	protected void initRoot() {
		if (root == null) {
			setRoot(makeRoot());
		}
	}

	protected abstract Parent makeRoot();

}
