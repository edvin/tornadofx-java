package tornadofx;

@FunctionalInterface
public interface ThrowableRunnable {
	void run() throws Exception;
}
