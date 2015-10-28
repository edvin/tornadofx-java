package tornadofx;

public abstract class FXEvent {
	private static final ThreadLocal<ListenerContext> context = new ThreadLocal<>();
	private Injectable source;

	public Injectable getSource() {
		return source;
	}

	void setSource(Injectable source) {
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
