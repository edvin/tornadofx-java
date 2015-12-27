package tornadofx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
class FXResources {
	static List<String> stylesheets = new ArrayList<>();

	static <NodeType extends Node> NodeType loadFxml(UIComponent<NodeType> component) throws IOException {
		Class componentType = component.getClass();

		URL fxml = componentType.getResource(componentType.getSimpleName().concat(".fxml"));

		if (fxml != null) {
			FXMLLoader loader = new FXMLLoader(fxml);
			loader.setController(component);
			return loader.load();
		}

		return null;
	}

	static boolean loadStyles(UIComponent component) {
		Class componentType = component.getClass();

		URL css = componentType.getResource(componentType.getSimpleName().concat(".css"));

		if (css != null && component.getNode() instanceof Parent) {
			Parent parent = (Parent) component.getNode();

			String stylesheet = css.toExternalForm();
			parent.getStylesheets().add(stylesheet);
			stylesheets.add(stylesheet);

			return true;
		}

		return false;
	}

}
