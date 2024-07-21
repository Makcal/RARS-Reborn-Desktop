package rarsreborn;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class RarsApplication extends Application implements Runnable{
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(RarsApplication.class.getResource("design.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setMaximized(true);
        stage.setTitle("RARS REBORN");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void run() {
        launch();
    }
}