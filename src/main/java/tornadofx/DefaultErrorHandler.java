package tornadofx;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class DefaultErrorHandler extends Controller {

	@OnEvent
	public void onError(UIError event) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			String errorMessage = event.getError().getMessage();
			alert.setTitle(errorMessage != null ? errorMessage : "An error occured");
			alert.setResizable(true);
			StackTraceElement[] stacktrace = event.getError().getStackTrace();

			if (stacktrace != null && stacktrace.length > 0) {
				String pos = stacktrace[0].toString();
				alert.setHeaderText("Error in " + pos);
			} else {
				alert.setHeaderText("Error in " + event.getSource());
			}

			TextArea textarea = new TextArea();
			textarea.setPrefRowCount(20);
			textarea.setPrefColumnCount(50);
			textarea.setText(stringFromError(event.getError()));

			Label cause = new Label(event.getError().getCause() != null ? event.getError().getCause().getMessage() : "");
			cause.setStyle("-fx-font-weight: bold");

			VBox content = new VBox();
			content.getChildren().addAll(cause, textarea);

			alert.getDialogPane().setContent(content);
			alert.showAndWait();
		});
	}

	private String stringFromError(Throwable e) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(out);
		e.printStackTrace(writer);
		writer.close();
		return out.toString();
	}

}
