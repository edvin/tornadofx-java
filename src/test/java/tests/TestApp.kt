package tests

import javafx.scene.Node
import tests.views.MyKotlinView
import tornadofx.App
import tornadofx.View

public class TestApp : App() {

    override fun getRootViewClass(): Class<View<Node>> =
        MyKotlinView::class.java as Class<View<Node>>
}
