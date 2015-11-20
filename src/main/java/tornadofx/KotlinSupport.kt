package tornadofx

import javafx.collections.ObservableList
import javafx.scene.Node
import kotlin.reflect.KClass

inline fun <reified T : Fragment<out Node>> fragment(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : Controller> controller(): T =
        InjectionContext.get(T::class.java)

inline fun <reified T : View<out Node>> view(): T =
        InjectionContext.get(T::class.java)

fun <Model : JsonModel> Rest.JsonObjectResult.to(objectType: KClass<Model>) : Model =
        to(objectType.java)

fun <Model : JsonModel> Rest.JsonArrayResult.to(objectType: KClass<Model>) : ObservableList<Model> =
        to(objectType.java)
