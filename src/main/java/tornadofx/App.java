package tornadofx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@SuppressWarnings("unchecked")
public abstract class App extends Application {

	public abstract Class<? extends View<? extends Pane>> getRootViewClass();

	public Class<? extends Component> getErrorHandlerClass() {
		return DefaultErrorHandler.class;
	}

	public void start(Stage stage) throws Exception {
		InjectionContext.get(getErrorHandlerClass());

		View<Pane> rootView = InjectionContext.get((Class<View<Pane>>) getRootViewClass());

		stage.titleProperty().bind(rootView.titleProperty());

		Pane parent = rootView.getNode();

		if (parent == null) {
			parent = new StackPane();
			parent.getChildren().add(new Label("Failed to create root view, see log for details."));
		}

		Scene scene = createScene(parent, stage);
		stage.setScene(scene);
		stageReady(stage);

		Platform.setImplicitExit(getImplicitExit());
	}

	public void stageReady(Stage stage) {
		stage.show();
	}

	public Scene createScene(Parent parent, Stage stage) {
		return new Scene(parent, getInitialWidth(stage), getInitialHeight(stage));
	}

	public double getInitialWidth(Stage stage) {
		return Math.min(1024, stage.getWidth());
	}

	public double getInitialHeight(Stage stage) {
		return Math.min(768, stage.getHeight());
	}

	public boolean getImplicitExit() {
		return true;
	}
}