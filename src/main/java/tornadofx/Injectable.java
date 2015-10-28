package tornadofx;

import javafx.concurrent.Task;
import javafx.scene.Node;

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

}
