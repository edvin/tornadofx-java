package tornadofx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

public abstract class Fragment<NodeType extends Node> extends UIComponent<NodeType> implements EventCapable {
	@Getter private Stage modalStage;

	public void openModal() {
		if (modalStage != null)
			return;

		if (!(getNode() instanceof Parent))
			throw new IllegalArgumentException("Only Parent Fragments can be opened in a Modal");

		modalStage = new Stage();
		modalStage.titleProperty().bind(titleProperty());
		modalStage.initModality(Modality.APPLICATION_MODAL);

		Scene scene = new Scene((Parent) getNode());

		scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ESCAPE)
				closeModal();
		});

		modalStage.setScene(scene);
		modalStage.show();

		Platform.runLater(() -> getNode().requestFocus());
	}

	public void closeModal() {
		if (modalStage != null) {
			modalStage.close();
			modalStage = null;
		}
	}

}
