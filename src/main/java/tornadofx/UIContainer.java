package tornadofx;

import javafx.scene.Node;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UIContainer {
	/**
	 * The name of the container, used as targets for docking operations. If no value, default to the field name.
	 *
	 * @see View#onDock(View, Node)
	 * @see View#dockIn(String)
	 * @see View#onChildDocked(UIComponent, Node)
	 */
	String name() default "";

	/**
	 * List of UI Components (Views and Fragments) to dock in this container when it is initialized
	 */
	Class<? extends UIComponent>[] load() default {};

	/**
	 * View Targets either support multiple child Components or just one. For Single View Targets, existing
	 * children will be removed when a new child is added and the appropriate lifecycle events will be called (onHide)
	 *
	 * @see View#onUndock(View, Node)
	 */
	boolean multi() default true;

	/**
	 * Update the title of the owning component when a child is docked.
	 * Note also that the Root View title is automatically bound to the Stage title,
	 * so docking in view targets inside the Root View with this property set to true will
	 * also update the Stage title.
	 */
	boolean updateTitle() default false;
}