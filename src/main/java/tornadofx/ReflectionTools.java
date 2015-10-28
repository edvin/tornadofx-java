package tornadofx;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
class ReflectionTools {
	static <T> T getFieldValue(Object object, Field field) {
		if (!field.isAccessible())
			field.setAccessible(true);
		try {
			return (T) field.get(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	static void setFieldValue(Object object, Field field, Object value) {
		if (!field.isAccessible())
			field.setAccessible(true);
		try {
			field.set(object, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	static ObservableList getFxChildren(Node node) {
		if (node instanceof TabPane)
			return ((TabPane) node).getTabs();
		else if (node instanceof Pane)
			return ((Pane) node).getChildren();

		return null;
	}

	static View getFxComponent(Object fxElem) {
		if (fxElem instanceof Node)
			return (View) ((Node) fxElem).getProperties().get("fxcomponent");
		else if (fxElem instanceof Tab)
			return (View) ((Tab) fxElem).getContent().getProperties().get("fxcomponent");
		return null;
	}

	static <Type> Type create(Class<Type> type) {
		try {
			return type.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static void invoke(Object owner, Method method, Object... arguments) throws Exception {
		if (!method.isAccessible())
			method.setAccessible(true);

		try {
			method.invoke(owner, arguments);
		} catch (InvocationTargetException ex) {
			if (ex.getCause() instanceof Exception)
				throw (Exception) ex.getCause();
			else
				throw new RuntimeException(ex);
		}
	}
}