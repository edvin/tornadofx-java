package tornadofx;

import javafx.scene.Node;
import javafx.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static tornadofx.ReflectionTools.setFieldValue;

@SuppressWarnings("unchecked")
class InjectionContext {
	private static final Map<Class, Component> singletons = new HashMap<>();

	@SuppressWarnings("unchecked")
	static <InjectableType extends Component> InjectableType get(Class<InjectableType> type) {
		if (singletons.containsKey(type))
			return (InjectableType) singletons.get(type);

		List<Component> components = new ArrayList<>();

		Set<Class> injectableTypes = InjectionContext.scanForInjectables(type);

		for (Class it : injectableTypes) {
			Pair<Component, Boolean> result = lookupOrCreate(it);

			Component component = result.getKey();
			boolean wasCreated = result.getValue();

			if (wasCreated)
				components.add(component);
		}

		components.stream()
			.forEach(component -> {
				if (component.isView())
					FX.registerUIContainers(component.toView());

				if (EventCapable.class.isAssignableFrom(component.getClass()))
					registerEventListeners((EventCapable) component);

				inject(component);
			});

		components.forEach(InjectionContext::postConstruct);

		components.stream().filter(Component::isUIComponent).map(Component::toUIComponent).forEach(FX::fillUIContainers);

		components.forEach(InjectionContext::postInit);

		return (InjectableType) components.get(0);
	}

	static Set<Class> scanForInjectables(Class type) {
		Set<Class> injectables = new LinkedHashSet<>();
		scanForInjectables(injectables, type);
		return injectables;
	}

	static Pair<Component, Boolean> lookupOrCreate(Class<? extends Component> componentType) {
		Component injectable;
		Boolean constructed;

		if (Fragment.class.isAssignableFrom(componentType)) {
			injectable = ReflectionTools.create(componentType);
			constructed = true;
		} else {
			if (singletons.containsKey(componentType)) {
				injectable = singletons.get(componentType);
				constructed = false;
			} else {
				injectable = ReflectionTools.create(componentType);
				singletons.put(componentType, injectable);
				constructed = true;
			}
		}

		if (constructed && injectable.isUIComponent()) {
			UIComponent component = (UIComponent) injectable;

			catchAndPublishError(component, () -> {
				Node node = component.createNode();

				if (node == null)
					throw new IllegalArgumentException(String.format("%s did not return a Node from createView", injectable));

				component.nodeProperty().setValue(node);
				FXResources.loadStyles(component);
				node.getProperties().put("fxcomponent", injectable);
			});
		}

		return new Pair(injectable, constructed);
	}

	static void catchAndPublishError(Component source, ThrowableRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception ex) {
			EventBus.publishError(source, ex);
		}
	}

	static void postInit(Component component) {
		catchAndPublishError(component, component::postInit);
	}

	static void postConstruct(Component component) {
		catchAndPublishError(component, () -> {
			component.postConstruct();

			ComponentConstructed constructedEvent = new ComponentConstructed();
			constructedEvent.setSource(component);
			EventBus.publish(constructedEvent);
		});
	}

	static void registerEventListeners(EventCapable eventCapable) {
		for (Method method : eventCapable.getClass().getDeclaredMethods()) {
			OnEvent onEvent = method.getAnnotation(OnEvent.class);

			if (onEvent != null && method.getParameterCount() == 1 && FXEvent.class.isAssignableFrom(method.getParameterTypes()[0]))
				EventBus.subscribe(eventCapable, method, onEvent);
		}
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	static void inject(Component component) {
		for (Field field : component.getClass().getDeclaredFields()) {
			if (Component.class.isAssignableFrom(field.getType())) {
				Inject inject = field.getAnnotation(Inject.class);

				if (inject != null && Component.class.isAssignableFrom(field.getType()))
					setFieldValue(component, field, get((Class<Component>)field.getType()));
			}
		}
	}

	private static void scanForInjectables(Set<Class> injectables, Class<? extends Component> type) {
		if (injectables.contains(type))
			return;

		injectables.add(type);

		Requires requires = type.getAnnotation(Requires.class);
		if (requires != null)
			for (Class<? extends Component> dependency : requires.value())
				scanForInjectables(injectables, dependency);

		for (Field field : type.getDeclaredFields()) {
			Inject inject = field.getAnnotation(Inject.class);

			if (inject != null && Component.class.isAssignableFrom(field.getType())) {
				Class<? extends Component> fieldType = (Class<? extends Component>) field.getType();
				scanForInjectables(injectables, fieldType);
				injectables.add(fieldType);
			}
		}
	}

}