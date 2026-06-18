package client.gui;

import client.NetworkClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class JavaFXMain extends Application {
    private NetworkClient client;

    @Override
    public void start(Stage primaryStage) {
        client = new NetworkClient("localhost", 8047);

        // Сразу пытаемся подключиться
        new Thread(() -> {
            try {
                client.connect();
                if (client.testConnection()) {
                    Platform.runLater(() -> {
                        LoginStage loginStage = new LoginStage(client);
                        loginStage.show();
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер недоступен");
                        alert.showAndWait();
                        Platform.exit();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка подключения: " + e.getMessage());
                    alert.showAndWait();
                    Platform.exit();
                });
            }
        }).start();
    }

    @Override
    public void stop() {
        if (client != null) client.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}