package tornadofx;

import javafx.concurrent.Task;
import javafx.scene.Node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

abstract class Injectable {
	public boolean isUIComponent() {
		return UIComponent.class.isAssignableFrom(getClass());
	}

	public boolean isView() {
		return View.class.isAssignableFrom(getClass());
	}
	public View toView() {
		return (View) this;
	}

	protected void fire(FXEvent event) {
		event.setSource(this);
		EventBus.publish(event);
	}

	protected void postConstruct() throws Exception {

	}

	protected void postInit() throws Exception {

	}

	protected <T> Task<T> async(Callable<T> backroundOperation, Consumer<T> uiConsumer) {
		return async(backroundOperation, uiConsumer, null);
	}

	protected <T> Task<T> async(Callable<T> backroundOperation) {
		return async(backroundOperation, null, null);
	}

	protected <T> Task<T> async(Callable<T> backroundOperation, Consumer<T> uiConsumer, Consumer<Throwable> errorConsumer) {
		Task<T> task = new Task<T>() {
			protected T call() throws Exception {
				return backroundOperation.call();
			}
		};

		if (uiConsumer != null)
			task.setOnSucceeded(event -> uiConsumer.accept(task.getValue()));

		if (errorConsumer != null)
			task.setOnFailed(event -> errorConsumer.accept(task.getException()));
		else
			task.setOnFailed(event -> EventBus.publishError(this, event.getSource().getException()));

		FX.executor.submit(task);
		return task;
	}

	@SuppressWarnings("unchecked")
	protected <NodeType extends Node, ViewType extends View> ViewType findView(Class<ViewType> componentType) {
		return InjectionContext.get(componentType);
	}

	@SuppressWarnings("unchecked")
	protected  <NodeType extends Node, FragmentType extends Fragment> FragmentType createFragment(Class<FragmentType> fragmentType) {
		return InjectionContext.get(fragmentType);
	}

	protected  <ControllerType extends Controller> ControllerType findController(Class<ControllerType> type) throws Exception {
		return InjectionContext.get(type);
	}

	public void setProperty(String key, Boolean value) {
		Properties properties = getProperties();
		properties.put(key, value.toString());
		saveProperties(properties);
	}

	public void setProperty(String key, Integer value) {
		Properties properties = getProperties();
		properties.put(key, value.toString());
		saveProperties(properties);
	}

	public void setProperty(String key, String value) {
		Properties properties = getProperties();
		properties.put(key, value);
		saveProperties(properties);
	}

	public boolean getBooleanProperty(String key) {
		return Boolean.valueOf(getProperty(key, "false"));
	}

	public Integer getIntProperty(String key) {
		return Integer.valueOf(getProperty(key));
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return getProperties().getProperty(key, defaultValue);
	}

	private void saveProperties(Properties properties) {
		try (OutputStream output = Files.newOutputStream(getPropertyPath())) {
			properties.store(output, "");
		} catch (IOException saveFailed) {
			fire(new UIError(saveFailed));
		}
	}

	private void removeProperty(String key) {
		Properties properties = getProperties();
		properties.remove(key);

		try {
			if (properties.isEmpty())
				Files.deleteIfExists(getPropertyPath());
			else
				saveProperties(properties);

		} catch (IOException ex) {
			fire(new UIError(ex));
		}
	}
	private Properties getProperties() {
		Properties properties = new Properties();

		Path path = getPropertyPath();

		if (Files.exists(path)) {
			try (InputStream input = Files.newInputStream(path)) {
				properties.load(input);
			} catch (IOException ex) {
				fire(new UIError(ex));
			}
		}

		return properties;
	}

	private Path getPropertyPath() {
		return Paths.get(getClass().getName().concat(".tornadofx.properties"));
	}
}
