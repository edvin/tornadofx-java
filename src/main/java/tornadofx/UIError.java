package tornadofx;

public class UIError extends FXEvent {
	private final Throwable error;

	public UIError(Throwable error) {
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}
}
