package tornadofx

import javafx.scene.Node
import kotlin.reflect.KClass

fun <NodeType : Node, FragmentType : Fragment<NodeType>> createFragment(type: KClass<FragmentType>): FragmentType =
        InjectionContext.get(type.java)

fun <ControllerType : Controller> lookupController(type: KClass<ControllerType>): ControllerType =
        InjectionContext.get(type.java)

fun <NodeType : Node, ViewType : View<NodeType>> lookupView(componentType: Class<ViewType>): ViewType =
        InjectionContext.get(componentType)
