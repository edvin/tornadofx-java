package tornadofx;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class Component {
	public boolean isUIComponent() {
		return UIComponent.class.isAssignableFrom(getClass());
	}

	public boolean isView() {
		return View.class.isAssignableFrom(getClass());
	}
	public View toView() {
		return (View) this;
	}
	public UIComponent toUIComponent() { return (UIComponent) this; }

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

	protected Task<Void> async(ThrowableRunnable backroundOperation) {
		return async(() -> { backroundOperation.run(); return null; }, null, null);
	}

	protected <T> Task<T> async(Callable<T> backroundOperation, Consumer<T> successHandler, Consumer<Throwable> errorHandler) {
		Task<T> task = new Task<T>() {
			protected T call() throws Exception {
				return backroundOperation.call();
			}
		};

		if (successHandler != null)
			task.setOnSucceeded(event -> {
				try {
					successHandler.accept(task.getValue());
				} catch (Exception ex) {
					fire(new UIError(ex));
				}
			});

		if (errorHandler != null)
			task.setOnFailed(event -> {
				try {
					errorHandler.accept(task.getException());
				} catch (Exception ex) {
					fire(new UIError(ex));
				}
			});
		else
			task.setOnFailed(event -> EventBus.publishError(this, event.getSource().getException()));

		FX.executor.submit(task);
		return task;
	}

	@SuppressWarnings("unchecked")
	public static <NodeType extends ViewType, ViewType extends View> ViewType lookupView(Class<ViewType> componentType) {
		return InjectionContext.get(componentType);
	}

	@SuppressWarnings("unchecked")
	public static <NodeType extends Node, FragmentType extends Fragment<NodeType>> FragmentType createFragment(Class<FragmentType> fragmentType) {
		return InjectionContext.get(fragmentType);
	}

	public static <ControllerType extends Controller> ControllerType lookupController(Class<ControllerType> type) {
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

	public void setProperty(String key, Double value) {
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
		String value = getProperty(key);
		return value == null ? null : Integer.valueOf(value);
	}

	public Double getDoubleProperty(String key) {
		String value = getProperty(key);
		return value == null ? null : Double.valueOf(value);
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return getProperties().getProperty(key, defaultValue);
	}

	public void saveProperties(Properties properties) {
		try (OutputStream output = Files.newOutputStream(getPropertyPath())) {
			properties.store(output, "");
		} catch (IOException saveFailed) {
			fire(new UIError(saveFailed));
		}
	}

	public void removeProperty(String key) {
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

	public Properties getProperties() {
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
        Path conf = Paths.get("conf");

        if (!Files.exists(conf))
            try {
                Files.createDirectories(conf);
            } catch (IOException e) {
                fire(new UIError(e));
            }

        return conf.resolve(getClass().getName().concat(".properties"));
	}

	public Stage getPrimaryStage() {
		return FX.primaryStage;
	}

    public static <T extends UIComponent> T getComponent(Node node) {
        return (T) node.getProperties().get("fxcomponent");
    }

    public static UIComponent getComponent(Tab tab) {
	    Node content = tab.getContent();
	    return content == null ? null : getComponent(content);
    }

}
