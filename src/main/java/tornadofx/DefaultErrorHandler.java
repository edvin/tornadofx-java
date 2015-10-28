package tornadofx;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

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
			String pos = event.getError().getStackTrace()[0].toString();
			alert.setHeaderText("Error in " + pos);
			TextArea textarea = new TextArea();
			textarea.setPrefRowCount(20);
			textarea.setPrefColumnCount(50);
			textarea.setText(stringFromError(event.getError()));
			alert.getDialogPane().setContent(textarea);
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
