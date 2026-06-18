package client.gui;

import client.NetworkClient;
import client.i18n.I18nManager;
import core.CommandRequest;
import core.CommandResponse;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginStage extends Stage {

    private final NetworkClient client;
    private final TextField loginField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button();
    private final Button registerButton = new Button();
    private final Label statusLabel = new Label();

    public LoginStage(NetworkClient client) {
        this.client = client;
        setTitle("Lab8 Client");
        initModality(Modality.APPLICATION_MODAL);

        loginField.setUserData("login.label");
        passwordField.setUserData("password.label");
        loginButton.setUserData("button.login");
        registerButton.setUserData("button.register");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(300, 200);

        loginField.setPromptText("");
        passwordField.setPromptText("");

        HBox buttonBox = new HBox(10, loginButton, registerButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                loginField,
                passwordField,
                buttonBox,
                statusLabel
        );

        loginButton.setOnAction(e -> login());
        registerButton.setOnAction(e -> register());

        setScene(new Scene(root));

        I18nManager.getInstance().updateAllTexts(getScene().getRoot());
        setTitle(I18nManager.getInstance().getString("title.login"));
    }

    private void login() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        I18nManager i18n = I18nManager.getInstance();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText(i18n.getString("error.emptyFields"));
            return;
        }

        CommandRequest request = new CommandRequest("help", null, login, password);

        Task<CommandResponse> task = new Task<>() {
            @Override
            protected CommandResponse call() throws Exception {
                return client.sendRequest(request);
            }
        };

        task.setOnSucceeded(event -> {
            CommandResponse resp = task.getValue();
            if (resp.getString().contains("Ошибка аутентификации")) {
                statusLabel.setText(i18n.getString("error.auth"));
            } else {
                client.setCredentials(login, password);

                Task<Integer> whoAmITask = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        CommandResponse resp = client.sendRequest(
                                new CommandRequest("whoami", null, login, password));
                        if (resp.getData() instanceof Integer id) {
                            return id;
                        }
                        throw new Exception("Не удалось получить userId");
                    }
                };
                whoAmITask.setOnSucceeded(ev -> {
                    client.setCurrentUserId(whoAmITask.getValue());
                    close();
                    MainStage mainStage = new MainStage(client);
                    mainStage.show();
                    mainStage.startPolling();
                });
                whoAmITask.setOnFailed(ev -> {
                    statusLabel.setText(i18n.getString("error.network"));
                });
                new Thread(whoAmITask).start();
            }
        });

        task.setOnFailed(event -> {
            statusLabel.setText(i18n.getString("error.network"));
        });

        new Thread(task).start();
    }

    private void register() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        I18nManager i18n = I18nManager.getInstance();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText(i18n.getString("error.emptyFields"));
            return;
        }

        java.util.Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("login", login);
        args.put("password", password);
        CommandRequest request = new CommandRequest("register", args, login, password);

        Task<CommandResponse> task = new Task<>() {
            @Override
            protected CommandResponse call() throws Exception {
                return client.sendRequest(request);
            }
        };

        task.setOnSucceeded(event -> {
            CommandResponse resp = task.getValue();
            if (resp.getString().contains("успешна")) {
                statusLabel.setText(i18n.getString("register.success"));
            } else {
                statusLabel.setText(resp.getString());
            }
        });

        task.setOnFailed(event -> {
            statusLabel.setText(i18n.getString("error.network"));
        });

        new Thread(task).start();
    }
}