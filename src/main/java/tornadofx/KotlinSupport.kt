package tornadofx

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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

inline fun <T> List<T>.observable() = FXCollections.observableList(this)

@Suppress("UNCHECKED_CAST")
inline public fun <reified T : Component> inject(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
    override fun getValue(thisRef: Component, property: KProperty<*>): T {
        return InjectionContext.get(T::class.java)
    }
}
