package tornadofx;

public abstract class FXEvent {
	private static final ThreadLocal<ListenerContext> context = new ThreadLocal<>();
	private Component source;

	public Component getSource() {
		return source;
	}

	void setSource(Component source) {
		this.source = source;
	}

	void resetContext() {
		context.set(new ListenerContext());
	}

	public ListenerContext getContext() {
		return context.get();
	}

	public static final class ListenerContext {
		private boolean removeListener = false;

		public boolean isRemoveListener() {
			return removeListener;
		}

		public void setRemoveListener(boolean removeListener) {
			this.removeListener = removeListener;
		}
	}
}
