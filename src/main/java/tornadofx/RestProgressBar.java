package tornadofx;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.stream.Collectors;

public class RestProgressBar extends View<ProgressBar> {
	@Inject Rest api;

	protected void postConstruct() throws Exception {
		api.ongoingRequests.addListener((ListChangeListener<HttpRequestBase>) c -> {
			int size = c.getList().size();

			Platform.runLater(() -> {
				ProgressBar indicator = getNode();

                String tooltip = c.getList().stream()
                        .map(r -> String.format("%s %s", r.getMethod(), r.getURI()))
                        .collect(Collectors.joining("\n"));

                indicator.setTooltip(new Tooltip(tooltip));
                indicator.setVisible(size > 0);

				if (size == 0) {
					indicator.setProgress(100);
				} else if (size == 1) {
					indicator.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
				} else {
					double pct = 1d / (double) size;
					indicator.setProgress(pct);
				}
			});
		});
	}

	protected ProgressBar createNode() throws IOException {
		ProgressBar indicator = new ProgressBar();
		indicator.setPrefWidth(100);
		indicator.setVisible(false);
		return indicator;
	}
}
