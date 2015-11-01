package tornadofx;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;

@SuppressWarnings("unused")
public abstract class UIComponent<NodeType extends Node> extends Component {
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

	/**
	 * Execute action when the enter key is pressed or the mouse is double clicked
	 *
	 * @param node The node to attach the event to
	 * @param action The runnable to execute on select
	 */
	protected void onUserSelect(Node node, ThrowableRunnable action) {
		node.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			if (event.getClickCount() == 2)
				FX.errorReportingRunnable(this, action).run();
		});

		node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER && !event.isMetaDown())
				FX.errorReportingRunnable(this, action).run();
		});
	}

	/**
	 * Execute action when Shortcut-Enter is pressed inside the given Node
	 * @param node The node to attach the event to
	 * @param action The action to perform on Shortcut-Enter
	 */
	protected  void onShortcutEnter(Node node, ThrowableRunnable action) {
		node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER && event.isShortcutDown())
				FX.errorReportingRunnable(this, action).run();
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
			if (event.getCode() == KeyCode.BACK_SPACE)
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

	public void dockIn(String name, UIContainer container, Node node) {
		FX.dock(this, name);
	}

	public boolean isDocked() {
		return getNode().getParent() != null;
	}

	protected void onDock(UIComponent parent, Node node) {

	}

	protected void onUndock(UIComponent parent, Node node) {

	}

	public void onDockInTab(UIComponent view, TabPane tabPane, Tab tab) {

	}

	protected void onChildDocked(UIComponent child, Node parentNode) {

	}

	protected void onChildUndocked(UIComponent child, Node parentNode) {

	}

	public MenuItem addContextMenuItem(Control control, String text, Node graphic, EventHandler<ActionEvent> action) {
		MenuItem item = new MenuItem(text);
		item.setGraphic(graphic);
		item.setOnAction(action);

		addContextMenuItem(control, item);

		return item;
	}

	public void addContextMenuItem(Control control, MenuItem item) {
		ContextMenu menu = control.getContextMenu();

		if (menu == null) {
			menu = new ContextMenu();
			control.setContextMenu(menu);
		}

		menu.getItems().add(item);
	}
}
