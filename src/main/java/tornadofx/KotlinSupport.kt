package tornadofx

import javafx.collections.ObservableList
import javafx.scene.Node

inline fun <reified T : Fragment<out Node>> fragment(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : Controller> controller(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : View<out Node>> view(): T =
        InjectionContext.get(T::class.java)

inline fun <reified Model : JsonModel> Rest.JsonObjectResult.to() : Model =
        to(Model::class.java)

inline fun <reified Model : JsonModel> Rest.JsonArrayResult.to() : ObservableList<Model> =
        to(Model::class.java)
