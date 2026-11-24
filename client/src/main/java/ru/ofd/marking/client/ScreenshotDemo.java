package ru.ofd.marking.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import ru.ofd.marking.client.ui.MainView;

/**
 * Captures client screen snapshots for artifacts (run under xvfb if needed).
 */
public class ScreenshotDemo extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Path out = Path.of(System.getProperty("ofd.screenshots.dir", "artifacts/screenshots"));
        Files.createDirectories(out);
        ClientConfig config = ClientConfig.load();
        ApiClient api = new ApiClient(config);
        MainView root = new MainView(api, config);
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Доля маркированных чеков — ОФД");
        stage.setScene(scene);
        stage.show();
        root.refreshAll();

        new Thread(() -> {
            try {
                Thread.sleep(3500);
                String[] names = {
                        "01-dashboard", "02-worst", "03-groups", "04-matrix", "05-receipts", "06-settings"
                };
                for (int i = 0; i < names.length; i++) {
                    final int idx = i;
                    Platform.runLater(() -> {
                        root.selectTab(idx);
                    });
                    Thread.sleep(800);
                    Platform.runLater(() -> {
                        try {
                            WritableImage img = scene.snapshot(null);
                            File f = out.resolve(names[idx] + ".png").toFile();
                            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
                            System.out.println("Wrote " + f.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(500);
                }
                Thread.sleep(1000);
                Platform.exit();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        }, "shot").start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
