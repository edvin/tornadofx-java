package tornadofx;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class Fragment<NodeType extends Node> extends UIComponent<NodeType> {
	private Stage modalStage;

	public void openModal() {
		if (modalStage != null)
			return;

		if (!(getNode() instanceof Parent))
			throw new IllegalArgumentException("Only Parent Fragments can be opened in a Modal");

		modalStage = new Stage();
		modalStage.titleProperty().bind(titleProperty());
		modalStage.initModality(Modality.WINDOW_MODAL);

		Scene scene = new Scene((Parent) getNode());
		modalStage.setScene(scene);
		modalStage.show();

		getNode().requestFocus();

		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ESCAPE)
				closeModal();
		});

	}

	public void closeModal() {
		if (modalStage != null) {
			modalStage.close();
			modalStage = null;
		}
	}

}
