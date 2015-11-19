package tornadofx

import javafx.collections.ObservableList
import javafx.scene.Node
import kotlin.reflect.KClass

fun <NodeType : Node, FragmentType : Fragment<NodeType>> createFragment(type: KClass<FragmentType>): FragmentType =
        InjectionContext.get(type.java)

fun <ControllerType : Controller> lookupController(type: KClass<ControllerType>): ControllerType =
        InjectionContext.get(type.java)

fun <NodeType : Node, ViewType : View<NodeType>> lookupView(componentType: KClass<ViewType>): ViewType =
        InjectionContext.get(componentType.java)

fun <Model : JsonModel> Rest.JsonObjectResult.to(objectType: KClass<Model>) : Model =
        to(objectType.java)

fun <Model : JsonModel> Rest.JsonArrayResult.to(objectType: KClass<Model>) : ObservableList<Model> =
        to(objectType.java)
