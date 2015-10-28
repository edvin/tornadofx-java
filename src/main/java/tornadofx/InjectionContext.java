package tornadofx;

import javafx.scene.Node;
import javafx.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static tornadofx.ReflectionTools.setFieldValue;

@SuppressWarnings("unchecked")
class InjectionContext {
	private static final Map<Class, Injectable> singletons = new HashMap<>();

	@SuppressWarnings("unchecked")
	static <InjectableType extends Injectable> InjectableType get(Class<InjectableType> type) {
		if (singletons.containsKey(type))
			return (InjectableType) singletons.get(type);

		List<Injectable> injectables = new ArrayList<>();

		Set<Class> injectableTypes = InjectionContext.scanForInjectables(type);

		for (Class it : injectableTypes) {
			Pair<Injectable, Boolean> result = lookupOrCreate(it);

			Injectable injectable = result.getKey();
			boolean wasCreated = result.getValue();

			if (wasCreated)
				injectables.add(injectable);
		}

		injectables.stream()
			.forEach(injectable -> {
				if (injectable.isView())
					FX.registerViewTargets(injectable.toView());

				if (EventCapable.class.isAssignableFrom(injectable.getClass()))
					registerEventListeners((EventCapable) injectable);

				inject(injectable);
			});

		injectables.forEach(InjectionContext::postConstruct);

		injectables.forEach(InjectionContext::postInit);

		return (InjectableType) injectables.get(0);
	}

	static Set<Class> scanForInjectables(Class type) {
		Set<Class> injectables = new LinkedHashSet<>();
		scanForInjectables(injectables, type);
		return injectables;
	}

	static Pair<Injectable, Boolean> lookupOrCreate(Class<? extends Injectable> componentType) {
		Injectable injectable;
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

		if (injectable.isUIComponent()) {
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

	static void catchAndPublishError(Injectable source, ThrowableRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception ex) {
			EventBus.publishError(source, ex);
		}
	}

	static void postInit(Injectable injectable) {
		catchAndPublishError(injectable, injectable::postInit);
	}

	static void postConstruct(Injectable injectable) {
		catchAndPublishError(injectable, () -> {
			injectable.postConstruct();

			ComponentConstructed constructedEvent = new ComponentConstructed();
			constructedEvent.setSource(injectable);
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
	static void inject(Injectable injectable) {
		for (Field field : injectable.getClass().getDeclaredFields()) {
			if (Injectable.class.isAssignableFrom(field.getType())) {
				Inject inject = field.getAnnotation(Inject.class);
				if (inject != null)
					setFieldValue(injectable, field, singletons.get(field.getType()));
			}
		}
	}

	private static void scanForInjectables(Set<Class> injectables, Class<? extends Injectable> type) {
		if (injectables.contains(type))
			return;

		injectables.add(type);

		Requires requires = type.getAnnotation(Requires.class);
		if (requires != null)
			for (Class<? extends Injectable> dependency : requires.value())
				scanForInjectables(injectables, dependency);

		for (Field field : type.getDeclaredFields()) {
			Inject inject = field.getAnnotation(Inject.class);
			if (inject != null && Injectable.class.isAssignableFrom(field.getType())) {
				Class<? extends Injectable> fieldType = (Class<? extends Injectable>) field.getType();
				scanForInjectables(injectables, fieldType);
				injectables.add(fieldType);
			}
		}
	}

}