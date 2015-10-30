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

import static tornadofx.ReflectionTools.*;

@SuppressWarnings("unchecked")
public class FX {
	private static final Map<String, ViewTargetContainer> viewTargets = new HashMap<>();
	static final ExecutorService executor = Executors.newCachedThreadPool();
	static Stage primaryStage;

	public static void dock(UIComponent component, String target) {
		ViewTargetContainer container = viewTargets.get(target);
		dock(component, container);
	}

	private static void dock(UIComponent child, ViewTargetContainer container) {
		Node viewTarget = container.target;

		if (child.isDocked())
			child.undock();

		if (viewTarget instanceof Pane) {
			Pane pane = (Pane) viewTarget;

			if (!container.config.multi())
				pane.getChildren().clear();

			pane.getChildren().add(child.getNode());
		} else if (viewTarget instanceof TabPane) {
			TabPane tabPane = (TabPane) viewTarget;

			if (!container.config.multi())
				tabPane.getTabs().clear();

			Tab tab = new Tab();
			tab.setContent(child.getNode());

			tab.textProperty().bindBidirectional(child.titleProperty());
			tabPane.getTabs().add(tab);
			tabPane.getSelectionModel().select(tab);
		} else if (viewTarget instanceof ToolBar) {
			ToolBar toolBar = (ToolBar) viewTarget;

			if (!container.config.multi())
				toolBar.getItems().clear();

			toolBar.getItems().add(child.getNode());
		}

		if (container.config.updateTitle()) {
			container.component.titleProperty().unbind();
			container.component.titleProperty().bind(child.titleProperty());
		}

	}

	/**
	 * Register ViewTargets and hook listeners for onDock, onUndock, onChildDocked and onChildUndocked
	 * @param component The component to look for ViewTargets in
	 */
	static void registerViewTargets(View component) {
		for (Field field : component.getClass().getDeclaredFields()) {
			ViewTarget vt = field.getAnnotation(ViewTarget.class);

			if (vt != null && Node.class.isAssignableFrom(field.getType())) {
				Node viewTarget = getFieldValue(component, field);
				ViewTargetContainer container = new ViewTargetContainer(component, viewTarget, vt);
				String targetName = vt.value().isEmpty() ? field.getName() : vt.value();
				viewTargets.put(targetName, container);

				ObservableList children = getFxChildren(viewTarget);

				if (children != null)
					addLifeCycleListeners(component, viewTarget, children);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void addLifeCycleListeners(View component, Node viewTarget, ObservableList children) {
		ListChangeListener listener = change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					change.getAddedSubList().forEach(node -> {
						View child = getFxComponent(node);
						if (child != null) {
							child.onDock(component, viewTarget);
							child.docked.setValue(true);
							component.onChildDocked(child, viewTarget);
						}
					});
				}

				if (change.wasRemoved()) {
					change.getRemoved().forEach(node -> {
						View child = getFxComponent(node);
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
	 * @param source The target component
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
	 * @param source The source component
	 */
	static void submit(ThrowableRunnable invocation, Component source) {
		submit(invocation, source, Platform.isFxApplicationThread());
	}

	@SuppressWarnings("unchecked")
	private static class ViewTargetContainer {
		public UIComponent component;
		public Node target;
		public ViewTarget config;

		public ViewTargetContainer(UIComponent owner, Node target, ViewTarget config) {
			this.component = owner;
			this.target = target;
			this.config = config;
		}
	}
}