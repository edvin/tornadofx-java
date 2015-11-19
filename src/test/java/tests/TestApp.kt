package tests

import javafx.scene.layout.Pane
import tornadofx.App
import tests.views.MyKotlinView
import tornadofx.View

public class TestApp : App() {

    override fun <PaneType : Pane?, RootView : View<PaneType>?> getRootViewClass(): Class<RootView>? {
        return MyKotlinView::class.java as Class<RootView>
    }
}
