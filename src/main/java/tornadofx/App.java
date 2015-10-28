package tornadofx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

@SuppressWarnings("unchecked")
public abstract class App extends Application {

	public abstract Class<? extends View<? extends Pane>> getRootViewClass();

	public Class<? extends Injectable> getErrorHandlerClass() {
		return DefaultErrorHandler.class;
	}

	public void start(Stage stage) throws Exception {
		// Make sure error handler is ready
		InjectionContext.get(getErrorHandlerClass());

		// Instantiate root view
		View<Pane> root = InjectionContext.get((Class<View<Pane>>) getRootViewClass());

		stage.titleProperty().bind(root.titleProperty());

		Scene scene = createScene(root.getNode());
		stage.setScene(scene);

		stage.show();

		postInit(stage);

		Platform.setImplicitExit(getImplicitExit());
	}

	public void postInit(Stage stage) {
		stage.getScene().getAccelerators()
			.put(KeyCombination.valueOf("Shortcut+s"), () -> stage.getScene().lookup("#query").requestFocus());
	}

	public Scene createScene(Parent parent) {
		return new Scene(parent, getInitialWidth(), getInitialHeight());
	}

	public double getInitialWidth() {
		return 1024;
	}

	public double getInitialHeight() {
		return 768;
	}

	public boolean getImplicitExit() {
		return true;
	}
}