package tornadofx

import javafx.collections.ObservableList
import javafx.scene.Node

inline fun <reified T : Fragment<out Node>> fragment(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : Controller> controller(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : View<out Node>> view(): T =
        InjectionContext.get(T::class.java)

inline fun <reified Model : JsonModel> Rest.JsonObjectResult.toModel() : Model =
        toModel(Model::class.java)

inline fun <reified Model : JsonModel> Rest.JsonArrayResult.toModel() : ObservableList<Model> =
        toModel(Model::class.java)
