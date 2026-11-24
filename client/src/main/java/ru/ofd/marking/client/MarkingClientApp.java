package ru.ofd.marking.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.ofd.marking.client.ui.MainView;

public class MarkingClientApp extends Application {

    @Override
    public void start(Stage stage) {
        ClientConfig config = ClientConfig.load();
        ApiClient api = new ApiClient(config);
        MainView root = new MainView(api, config);
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Доля маркированных чеков — ОФД");
        stage.setScene(scene);
        stage.show();
        root.refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
