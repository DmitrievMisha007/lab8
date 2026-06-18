package client.gui;

import client.NetworkClient;
import client.gui.pages.*;
import client.i18n.I18nManager;
import core.CommandRequest;
import core.CommandResponse;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import client.gui.components.TicketDialog;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainStage extends Stage {
    private final NetworkClient client;
    private final TabPane tabPane = new TabPane();
    private final TablePage tablePage;
    private final VisualizationPage visPage;
    private final CommandPage commandPage;

    private final Tab tabTable = new Tab();
    private final Tab tabVisual = new Tab();
    private final Tab tabCommands = new Tab();

    private List<Map<String, Object>> currentTickets = new ArrayList<>();
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
    private final I18nManager i18n = I18nManager.getInstance();

    private final MenuButton langMenu = new MenuButton();


    public MainStage(NetworkClient client) {
        this.client = client;

        tablePage = new TablePage(client, this::refreshAll);
        visPage = new VisualizationPage(client, this::refreshAll);
        commandPage = new CommandPage(client);
        commandPage.addRefreshCallback(this::refreshAll);

        HBox topPanel = new HBox(10);
        topPanel.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0;");

        Label userLabel = new Label();
        userLabel.setUserData("label.user");
        Button addButton = new Button();
        addButton.setUserData("button.add");
        addButton.setOnAction(e -> addNewTicket());

        Button clearButton = new Button();
        clearButton.setUserData("button.clear");
        clearButton.setOnAction(e -> clearMyTickets());

        Image flagRu = new Image(getClass().getResourceAsStream("/flags/ru.png"));
        Image flagRo = new Image(getClass().getResourceAsStream("/flags/ro.png"));
        Image flagUk = new Image(getClass().getResourceAsStream("/flags/uk.png"));
        Image flagEs = new Image(getClass().getResourceAsStream("/flags/es_pr.png"));

        final double flagWidth = 24;
        final double flagHeight = 24;

        ImageView ruView = new ImageView(flagRu);
        ruView.setFitHeight(flagWidth); ruView.setFitWidth(flagHeight);
        ImageView roView = new ImageView(flagRo);
        roView.setFitHeight(flagWidth); roView.setFitWidth(flagHeight);
        ImageView ukView = new ImageView(flagUk);
        ukView.setFitHeight(flagWidth); ukView.setFitWidth(flagHeight);
        ImageView esView = new ImageView(flagEs);
        esView.setFitHeight(flagWidth); esView.setFitWidth(flagHeight);

        langMenu.setGraphic(ruView);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topPanel.getChildren().addAll(userLabel, addButton, clearButton, spacer, langMenu);

        MenuItem ruItem = new MenuItem("Русский", ruView);
        MenuItem roItem = new MenuItem("Română", roView);
        MenuItem ukItem = new MenuItem("Українська", ukView);
        MenuItem esItem = new MenuItem("Español PR", esView);

        ruItem.setOnAction(e -> { switchLocale(new Locale("ru"), flagRu); });
        roItem.setOnAction(e -> { switchLocale(new Locale("ro"), flagRo); });
        ukItem.setOnAction(e -> { switchLocale(new Locale("uk"), flagUk); });
        esItem.setOnAction(e -> { switchLocale(new Locale("es", "PR"), flagEs); });

        langMenu.getItems().addAll(ruItem, roItem, ukItem, esItem);

        tabTable.setContent(tablePage);
        tabTable.setUserData("tab.table");
        tabTable.setClosable(false);
        tabVisual.setContent(visPage);
        tabVisual.setUserData("tab.visual");
        tabVisual.setClosable(false);
        tabCommands.setContent(commandPage);
        tabCommands.setUserData("tab.commands");
        tabCommands.setClosable(false);
        tabPane.getTabs().addAll(tabTable, tabVisual, tabCommands);

        BorderPane root = new BorderPane();
        root.setTop(topPanel);
        root.setCenter(tabPane);
        setScene(new Scene(root, 1000, 700));

        updateTabTexts();
        i18n.updateAllTexts(getScene().getRoot());
    }

    private void updateTabTexts() {
        tabTable.setText(i18n.getString((String) tabTable.getUserData()));
        tabVisual.setText(i18n.getString((String) tabVisual.getUserData()));
        tabCommands.setText(i18n.getString((String) tabCommands.getUserData()));
    }

    public void startPolling() {
        poller.scheduleAtFixedRate(() -> {
            try {
                CommandRequest request = new CommandRequest("fetch", null,
                        client.getCurrentLogin(), client.getCurrentPassword());
                CommandResponse resp = client.sendRequest(request);
                if (resp.getData() instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resp.getData();
                    Platform.runLater(() -> {
                        currentTickets = list;
                        tablePage.refresh(list);
                        visPage.refresh(list);
                        commandPage.setCurrentTickets(list);
                    });
                }
            } catch (Exception e) {
                System.err.println("Ошибка поллинга: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void refreshAll() {
        refreshDataOnce();
    }

    private void refreshDataOnce() {
        new Thread(() -> {
            try {
                CommandRequest request = new CommandRequest("fetch", null,
                        client.getCurrentLogin(), client.getCurrentPassword());
                CommandResponse resp = client.sendRequest(request);
                if (resp.getData() instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resp.getData();
                    Platform.runLater(() -> {
                        currentTickets = list;
                        tablePage.refresh(list);
                        visPage.refresh(list);
                        commandPage.setCurrentTickets(list);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addNewTicket() {
        TicketDialog dialog = new TicketDialog(null);
        dialog.showAndWait().ifPresent(data -> {
            CommandRequest request = new CommandRequest("add", data,
                    client.getCurrentLogin(), client.getCurrentPassword());
            new Thread(() -> {
                try {
                    client.sendRequest(request);
                    Platform.runLater(this::refreshAll);
                } catch (Exception e) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                            "Ошибка добавления: " + e.getMessage()).show());
                }
            }).start();
        });
    }

    private void clearMyTickets() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, i18n.getString("confirm.clear"));
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                CommandRequest request = new CommandRequest("clear", null,
                        client.getCurrentLogin(), client.getCurrentPassword());
                new Thread(() -> {
                    try {
                        client.sendRequest(request);
                        Platform.runLater(this::refreshAll);
                    } catch (Exception e) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                                "Ошибка: " + e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    private void switchLocale(Locale locale, Image flagImage) {
        I18nManager.getInstance().setLocale(locale);
        I18nManager.getInstance().updateAllTexts(getScene().getRoot());
        updateTabTexts();
        tablePage.updateHeaderArrows();
        tablePage.updateContextMenuText();
        refreshDataOnce();

        ImageView newFlagView = new ImageView(flagImage);
        newFlagView.setFitWidth(24);
        newFlagView.setFitHeight(24);
        langMenu.setGraphic(newFlagView);
    }

    @Override
    public void close() {
        poller.shutdown();
        super.close();
    }
}