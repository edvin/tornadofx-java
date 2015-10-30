package tornadofx;

import javafx.scene.Node;

public abstract class View<NodeType extends Node> extends UIComponent<NodeType> implements EventCapable {

	protected void onDock(View view, Node node) {

	}

	protected void onUndock(View view, Node node) {

	}

	protected void onChildDocked(UIComponent child, Node parentNode) {

	}

	protected void onChildUndocked(UIComponent child, Node parentNode) {

	}

}
