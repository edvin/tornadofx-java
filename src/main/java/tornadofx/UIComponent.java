package tornadofx;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;

public abstract class UIComponent<NodeType extends Node> extends Injectable {
	private SimpleObjectProperty<NodeType> node = new SimpleObjectProperty<>();
	private SimpleStringProperty title = new SimpleStringProperty();
	ReadOnlyBooleanWrapper docked = new ReadOnlyBooleanWrapper();

	public NodeType getNode() {
		return node.get();
	}

	public SimpleObjectProperty<NodeType> nodeProperty() {
		return node;
	}

	public void setNode(NodeType node) {
		this.node.set(node);
	}

	public SimpleStringProperty titleProperty() {
		return title;
	}

	public void setTitle(String title) {
		this.title.set(title);
	}

	public ReadOnlyBooleanWrapper dockedProperty() {
		return docked;
	}

	protected void onDock(View view, Node node) {

	}

	protected void onUndock(View view, Node node) {

	}

	/**
	 * Execute action when the enter key is pressed or the mouse is double clicked
	 *
	 * @param node The node to attach the event to
	 * @param runnable The runnable to execute on select
	 */
	protected void onUserSelect(Node node, ThrowableRunnable runnable) {
		node.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			if (event.getClickCount() == 2)
				FX.errorReportingRunnable(this, runnable).run();
		});

		node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER && !event.isMetaDown())
				FX.errorReportingRunnable(this, runnable).run();
		});
	}

	/**
	 * Execute action when the delete key is pressed
	 *
	 * @param node The node to attach the event to
	 * @param runnable The runnable to execute on delete
	 */
	protected void onUserDelete(Node node, ThrowableRunnable runnable) {
		node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.DELETE)
				FX.errorReportingRunnable(this, runnable).run();
		});
	}

	public boolean undock() {
		Parent parent = getNode().getParent();

		if (parent == null)
			return false;

		if (parent.getParent() instanceof TabPane) {
			TabPane tabPane = (TabPane) parent.getParent();

			tabPane.getTabs().stream()
				.filter(tab -> tab.getContent().equals(getNode()))
				.findAny()
				.ifPresent(tab ->
					tabPane.getTabs().remove(tab));

		} else if (parent instanceof Pane) {
			Pane pane = (Pane) parent;
			pane.getChildren().remove(getNode());
			return true;
		}

		return false;
	}

	protected NodeType createNode() throws IOException {
		return FXResources.loadFxml(this);
	}

	public void dockIn(String viewTarget) {
		FX.dock(this, viewTarget);
	}

	public boolean isDocked() {
		return getNode().getParent() != null;
	}
}
