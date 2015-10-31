package tornadofx;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static tornadofx.ReflectionTools.getFieldValue;
import static tornadofx.ReflectionTools.getFxChildren;

@SuppressWarnings("unchecked")
public class FX {
	private static final Map<String, UIContainerRef> namedUIContainers = new HashMap<>();

	/**
	 * 	Executor uses daemon threads so the app will exit properly when the last window is closed.
	 * 	This executor should therefore not be used to run tasks that must complete before the app
	 * 	can exit after the last window is closed. For GUI app tasks this is probably a good default.
	 */
	static final ExecutorService executor = Executors.newCachedThreadPool(job -> {
		Thread thread = Executors.defaultThreadFactory().newThread(job);
		thread.setDaemon(true);
		return thread;
	});

	static Stage primaryStage;

	static void dock(UIComponent component, String name) {
		UIContainerRef ref = namedUIContainers.get(name);

		if (ref == null) {
			String error = String.format("Unable to dock %s in unknown UIContainer '%s'", component, name);
			EventBus.publishError(component, new IllegalArgumentException(error));
		} else {
			dock(component, ref.component, ref.node);
		}
	}

	static void dock(UIComponent child, UIComponent component, Node containerNode) {
		UIContainer config = null;

		for (Field field : component.getClass().getDeclaredFields()) {
			if (field.getType().isAssignableFrom(containerNode.getClass())) {
				if (containerNode.equals(ReflectionTools.getFieldValue(component, field))) {
					config = field.getAnnotation(UIContainer.class);
					break;
				}
			}
		}

		if (config == null) {
			String error = String.format("Unable to find UIContainer node %s in UIComponent %s", containerNode, component);
			EventBus.publishError(component, new IllegalArgumentException(error));
			return;
		}

		if (child.isDocked())
			child.undock();

		if (containerNode instanceof Pane) {
			Pane pane = (Pane) containerNode;

			if (!config.multi())
				pane.getChildren().clear();

			child.onDock(component, containerNode);
			child.docked.setValue(true);
			component.onChildDocked(child, containerNode);

			pane.getChildren().add(child.getNode());
		} else if (containerNode instanceof TabPane) {
			TabPane tabPane = (TabPane) containerNode;

			if (!config.multi())
				tabPane.getTabs().clear();

			Tab tab = new Tab();
			tab.setContent(child.getNode());

			tab.textProperty().bind(child.titleProperty());
			tabPane.getTabs().add(tab);


			child.onDockInTab(component, (TabPane) containerNode, tab);
			child.docked.setValue(true);
			component.onChildDocked(child, containerNode);

		} else if (containerNode instanceof ToolBar) {
			ToolBar toolBar = (ToolBar) containerNode;

			if (!config.multi())
				toolBar.getItems().clear();

			child.onDock(component, containerNode);
			child.docked.setValue(true);
			component.onChildDocked(child, containerNode);

			toolBar.getItems().add(child.getNode());
		}

		if (config.updateTitle()) {
			component.titleProperty().unbind();
			component.titleProperty().bind(child.titleProperty());
		}

	}

	/**
	 * Register UIContainers and hook listeners for onDock, onUndock, onChildDocked and onChildUndocked
	 * <p/>
	 * Make UIContainers available for dock operations by name.
	 * <p/>
	 * If name is not specified, the field name of the declaration is used.
	 *
	 * @param component The component to look for UIContainers in
	 */
	static void registerUIContainers(View component) {
		for (Field field : component.getClass().getDeclaredFields()) {
			UIContainer config = field.getAnnotation(UIContainer.class);

			if (config != null && Node.class.isAssignableFrom(field.getType())) {
				Node node = getFieldValue(component, field);

				UIContainerRef ref = new UIContainerRef(component, node);

				String name = config.name().isEmpty() ? field.getName() : config.name();

				if (namedUIContainers.containsKey(name)) {					String error = String.format("%s tried to register already occupied UIContainer name '%s'. " +
						"The registration was skipped. Change the name to avoid collision with %s.",
						component, name, namedUIContainers.get(name));

					EventBus.publishError(component, new IllegalArgumentException(error));
				} else {
					namedUIContainers.put(name, ref);
				}


				ObservableList children = getFxChildren(node);

				if (children != null)
					addLifeCycleListeners(component, node, children);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void addLifeCycleListeners(View component, Node viewTarget, ObservableList children) {
		ListChangeListener listener = change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					change.getRemoved().forEach(node -> {
						UIComponent child = node instanceof Node ? Component.getComponent((Node) node) : Component.getComponent((Tab) node);
						if (child != null) {
							child.onUndock(component, viewTarget);
							child.docked.setValue(false);
							component.onChildUndocked(child, viewTarget);
						}
					});
				}
			}
		};
		children.addListener(listener);
	}

	/**
	 * Run in correct Thread
	 *
	 * @param invocation The invocation to perform
	 * @param source     The target component
	 * @param isFxThread Are we running in the FX Thread?
	 */
	static void submit(ThrowableRunnable invocation, Component source, Boolean isFxThread) {
		if (isFxThread && !source.isUIComponent())
			FX.executor.submit(errorReportingRunnable(source, invocation));
		else if (!isFxThread && source.isUIComponent())
			Platform.runLater(errorReportingRunnable(source, invocation));
		else
			errorReportingRunnable(source, invocation).run();
	}

	static Runnable errorReportingRunnable(Component source, ThrowableRunnable runnable) {
		return () -> InjectionContext.catchAndPublishError(source, runnable);
	}

	/**
	 * Run in correct Thread
	 *
	 * @param invocation The invocation to perform
	 * @param source     The source component
	 */
	static void submit(ThrowableRunnable invocation, Component source) {
		submit(invocation, source, Platform.isFxApplicationThread());
	}

	/**
	 * Dock Views and Fragments into any registered UIContainer fields in the given View
	 */
	static void fillUIContainers(UIComponent component) {
		for (Field field : component.getClass().getDeclaredFields()) {

			UIContainer config = field.getAnnotation(UIContainer.class);

			if (config != null) {
				for (Class<? extends UIComponent> autoload : config.load()) {
					UIComponent child = InjectionContext.get(autoload);
					Node node = ReflectionTools.getFieldValue(component, field);
					dock(child, component, node);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static class UIContainerRef {
		public UIComponent component;
		public Node node;

		public UIContainerRef(UIComponent component, Node node) {
			this.component = component;
			this.node = node;
		}
	}
}