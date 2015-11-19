package tests.views

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import tornadofx.View

public class MyKotlinView : View<VBox>() {

    @FXML
    lateinit var saveButton: Button

    override fun postConstruct() {
        //        saveButton.setOnAction {
        //            Alert(Alert.AlertType.CONFIRMATION).apply {
        //                title = "Saved!"
        //                showAndWait()
        //            }
        //        }

        saveButton.setOnAction {
            async({ sayHello() }) {
                println(it)
            }
        }


    }

    public fun sayHello(): String = "Hello world"
}
