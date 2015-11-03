package tornadofx.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.xml.bind.JAXB;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Launcher extends Application {
    private static FXManifest manifest;
    private static Application app;

    public void start(Stage primaryStage) throws Exception {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);

        Label label = new Label("Updating...");
        label.setStyle("-fx-font-weight: bold");

        VBox root = new VBox(label, progressBar);
        root.setSpacing(10);
        root.setPadding(new Insets(25, 25, 25, 25));
        root.setFillWidth(true);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle(manifest.name);
        primaryStage.show();

        Task sync = sync();

        progressBar.progressProperty().bind(sync.progressProperty());

        sync.setOnSucceeded(e -> {
            try {
                app = launch(primaryStage);
            } catch (Exception initError) {
                reportError("Launch", initError);
            }
        });

        sync.setOnFailed(e -> reportError("Sync", sync.getException()));
        new Thread(sync).start();
    }


    public URLClassLoader createClassLoader() {
        List<URL> libs = manifest.files.stream().map(LibraryFile::toURL).collect(Collectors.toList());
        return new URLClassLoader(libs.toArray(new URL[libs.size()]));
    }

    public Application launch(Stage primaryStage) throws Exception {
        URLClassLoader classLoader = createClassLoader();
        Class<? extends Application> appclass = (Class<? extends Application>) classLoader.loadClass(manifest.launchClass);
        Thread.currentThread().setContextClassLoader(classLoader);
        Application app = appclass.newInstance();
        app.init();
        app.start(primaryStage);
        return app;
    }


    public Task sync() throws IOException {
        return new Task() {
            protected Object call() throws Exception {
                List<LibraryFile> needsUpdate = manifest.files.stream().filter(LibraryFile::needsUpdate).collect(Collectors.toList());
                Long totalBytes = needsUpdate.stream().mapToLong(f -> f.size).sum();
                Long totalWritten = 0L;

                for (LibraryFile lib : needsUpdate) {
                    updateMessage(lib.file.concat("..."));

                    Path target = Paths.get(lib.file).toAbsolutePath();
                    Files.createDirectories(target.getParent());

                    try (InputStream input = manifest.uri.resolve(lib.file).toURL().openStream();
                         OutputStream output = Files.newOutputStream(target)) {

                        byte[] buf = new byte[65536];

                        int read;
                        while ((read = input.read(buf)) > -1) {
                            output.write(buf, 0, read);
                            totalWritten += read;
                            updateProgress(totalWritten, totalBytes);
                        }
                    }
                }

                try (ByteArrayOutputStream mfstream = new ByteArrayOutputStream()) {
                    JAXB.marshal(manifest, mfstream);

                    Path manifestPath = Paths.get("fxapp.xml");
                    byte[] data = mfstream.toByteArray();
                    if (Files.notExists(manifestPath) || !Arrays.equals(Files.readAllBytes(manifestPath), data))
                        Files.write(manifestPath, data);
                }

                return null;
            }
        };
    }

    public void stop() throws Exception {
        if (app != null)
            app.stop();
    }

    private void reportError(String job, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to " + job + " application");
        alert.setHeaderText("There was an error during " + job + " of the application");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        error.printStackTrace(writer);
        writer.close();
        alert.setContentText(out.toString());

        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) throws Exception {
        URI uri = args.length > 0 ? URI.create(args[0]).resolve("fxapp.xml") : Paths.get("fxapp.xml").toUri();
        manifest = JAXB.unmarshal(uri, FXManifest.class);
        launch();
    }

}
