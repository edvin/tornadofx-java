package tornadofx;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

class EventBus {
	private static Map<Class<? extends FXEvent>, List<EventListener>> listeners = new HashMap<>();

	static void subscribe(EventCapable owner, Method consumer, OnEvent config) {
		EventListener listener = new EventListener(owner, consumer, config);
		getListenersForEventType(listener.getEventType()).add(listener);
	}

	static void unsubscribe(EventCapable owner, Method consumer, OnEvent config) {
		EventListener listener = new EventListener(owner, consumer, config);
		getListenersForEventType(listener.getEventType()).remove(listener);
	}

	static void publish(FXEvent event) {
		for (EventListener listener : getListenersForEventType(event.getClass()).stream().collect(Collectors.toList()))
			listener.dispatch(event);
	}

	@SuppressWarnings("unchecked")
	private static List<EventListener> getListenersForEventType(Class<? extends FXEvent> eventType) {
		List<EventListener> list = listeners.get(eventType);

		if (list == null) {
			list = new ArrayList<>();
			listeners.put(eventType, list);
		}

		return list;
	}

	public static void publishError(Injectable source, Throwable error) {
		UIError event = new UIError(error);
		event.setSource(source);
		publish(event);
	}

	@SuppressWarnings("unchecked")
	private static class EventListener {
		OnEvent config;
		EventCapable target;
		Method method;

		public EventListener(EventCapable target, Method consumer, OnEvent config) {
			this.target = target;
			method = consumer;
			this.config = config;
		}

		void dispatch(FXEvent event) {
			ThrowableRunnable invocation = () -> {
				event.resetContext();

				ReflectionTools.invoke(target, method, event);

				if (event.getContext().isRemoveListener())
					getListenersForEventType(event.getClass()).remove(this);
			};

			FX.submit(invocation, (Injectable) target);
		}

		Class<? extends FXEvent> getEventType() {
			return (Class<? extends FXEvent>) method.getParameterTypes()[0];
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof EventListener)) return false;
			EventListener that = (EventListener) o;
			return Objects.equals(target, that.target) &&
				Objects.equals(method, that.method);
		}

		public int hashCode() {
			return Objects.hash(target, method);
		}
	}
}
