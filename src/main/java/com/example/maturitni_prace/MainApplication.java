package com.example.maturitni_prace;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class MainApplication extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) {
        scene = new Scene(load("menu.fxml"), 900, 600);
        stage.setTitle("Počítadlo skóre");
        stage.setScene(scene);
        stage.show();
    }

    public static void show(String fxml) {
        scene.setRoot(load(fxml));
    }

    public static void showMenu() {
        show("menu.fxml");
    }

    private static Parent load(String fxml) {
        try {
            return FXMLLoader.load(MainApplication.class.getResource(fxml));
        } catch (IOException e) {
            throw new UncheckedIOException("Nelze načíst " + fxml, e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
