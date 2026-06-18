package client.gui.pages;

import client.NetworkClient;
import client.gui.components.TicketDialog;
import client.i18n.I18nManager;
import core.CommandRequest;
import core.CommandResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CommandPage extends VBox {
    private final NetworkClient client;
    private final TextField commandField = new TextField();
    private final Button sendButton = new Button();
    private final ListView<String> historyView = new ListView<>();
    private final ObservableList<String> historyList = FXCollections.observableArrayList();
    private final List<Runnable> refreshCallbacks = new ArrayList<>();

    private final List<String> historyInputs = new ArrayList<>();
    private int historyIndex = -1;
    private int historyViewIndex = -1;
    private boolean navigating = false;

    private List<Map<String, Object>> currentTickets = new ArrayList<>();

    private I18nManager i18n = I18nManager.getInstance();

    public CommandPage(NetworkClient client) {
        this.client = client;
        setPadding(new Insets(10));
        setSpacing(10);

        sendButton.setUserData("button.send");
        commandField.setUserData("prompt.command");
        I18nManager i18n = I18nManager.getInstance();
        sendButton.setText(i18n.getString("button.send"));
        commandField.setPromptText(i18n.getString("prompt.command"));

        commandField.textProperty().addListener((obs, oldText, newText) -> {
            if (!navigating) {
                historyIndex = -1;
                historyViewIndex = -1;
            }
        });

        commandField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP) {
                navigateHistory(1);  // назад
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                navigateHistory(-1); // вперёд
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeCommand();
                event.consume();
            }
        });
        sendButton.setOnAction(e -> executeCommand());

        historyView.setItems(historyList);
        historyView.setPrefHeight(400);
        getChildren().addAll(commandField, sendButton, historyView);
    }

    public void addRefreshCallback(Runnable callback) {
        refreshCallbacks.add(callback);
    }

    public void setCurrentTickets(List<Map<String, Object>> tickets) {
        this.currentTickets = tickets;
    }

    private void executeCommand() {
        String input = commandField.getText().trim();
        if (input.isEmpty()) return;

        historyInputs.add(input);
        historyViewIndex = -1;
        historyIndex = -1;

        String[] parts = input.split("\\s+", 2);
        String cmdName = parts[0];
        Map<String, Object> args = new LinkedHashMap<>();
        if (parts.length > 1) {
            args.put("arg1", parts[1]);
        }

        I18nManager i18n = I18nManager.getInstance();

        switch (cmdName) {
            case "add", "add_if_max", "add_if_min" -> {
                TicketDialog dialog = new TicketDialog(null);
                dialog.showAndWait().ifPresent(data -> {
                    CommandRequest request = new CommandRequest(cmdName, data,
                            client.getCurrentLogin(), client.getCurrentPassword());
                    sendRequest(request, input);
                });
                return;
            }
            case "update" -> {
                if (parts.length < 2) {
                    addHistory(i18n.getString("error.update") + " ID не указан");
                    return;
                }
                String idStr = parts[1];
                long id;
                try {
                    id = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    addHistory(i18n.getString("error.update") + " некорректный ID");
                    return;
                }
                Optional<Map<String, Object>> optTicket = currentTickets.stream()
                        .filter(t -> Long.valueOf(t.get("id").toString()) == id)
                        .findFirst();
                if (optTicket.isEmpty()) {
                    addHistory(i18n.getString("server.response.not_found"));
                    return;
                }
                Map<String, Object> oldData = optTicket.get();
                TicketDialog dialog = new TicketDialog(oldData);
                dialog.showAndWait().ifPresent(newData -> {
                    newData.put("arg1", idStr);
                    CommandRequest request = new CommandRequest("update", newData,
                            client.getCurrentLogin(), client.getCurrentPassword());
                    sendRequest(request, input);
                });
                return;
            }
            case "fetch" -> {
                CommandRequest request = new CommandRequest("fetch", null,
                        client.getCurrentLogin(), client.getCurrentPassword());
                sendRequest(request, input, true);
                return;
            }
            case "whoami" -> {
                CommandRequest request = new CommandRequest("whoami", null,
                        client.getCurrentLogin(), client.getCurrentPassword());
                sendRequest(request, input, true);
                return;
            }
            case "execute_script" -> {
                if (parts.length < 2) {
                    addHistory(i18n.getString("script.error.no_filename"));
                    return;
                }
                executeScript(parts[1]);
                return;
            }
        }

        CommandRequest request = new CommandRequest(cmdName, args.isEmpty() ? null : args,
                client.getCurrentLogin(), client.getCurrentPassword());
        sendRequest(request, input);
    }

    private void sendRequest(CommandRequest request, String userInput) {
        sendRequest(request, userInput, false);
    }

    private void sendRequest(CommandRequest request, String userInput, boolean isFetchOrWhoami) {
        new Thread(() -> {
            try {
                CommandResponse resp = client.sendRequest(request);
                Platform.runLater(() -> {
                    addHistory(localizeServerResponse(resp.getString()));
                    addHistory("> " + userInput);                         // команда
                    commandField.clear();
                    if (isModifyingCommand(request.getName())) {
                        refreshCallbacks.forEach(Runnable::run);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> addHistory("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private boolean isModifyingCommand(String cmd) {
        return Set.of("add", "add_if_max", "add_if_min", "update", "remove_by_id", "clear").contains(cmd);
    }

    private void addHistory(String entry) {
        historyList.add(0, entry);
        if (historyList.size() > 10) {
            historyList.remove(historyList.size() - 1);
        }
        historyView.scrollTo(historyList.size() - 1);
    }

    private String localizeServerResponse(String original) {
        I18nManager i18n = I18nManager.getInstance();
        if (original.contains("успешно добавлен")) return i18n.getString("server.response.add");
        if (original.contains("успешно обновлён")) return i18n.getString("server.response.update");
        if (original.contains("удалён")) return i18n.getString("server.response.remove");
        if (original.contains("Ваши билеты удалены")) return i18n.getString("server.response.clear");
        if (original.contains("не найден")) return i18n.getString("server.response.not_found");
        if (original.contains("Недостаточно прав")) return i18n.getString("server.response.no_rights");
        if (original.contains("не распознана")) return i18n.getString("server.response.unknown");
        if (original.contains("Ошибка")) return i18n.getString("server.response.error") + original.substring(original.indexOf(":"));
        return original;
    }

    private void executeScript(String fileName) {
        I18nManager i18n = I18nManager.getInstance();
        Path path = Paths.get(fileName);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            addHistory(i18n.getString("script.error.not_found"));
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] tokens = line.split("\\s+", 2);
                String cmd = tokens[0];
                Map<String, Object> args = new LinkedHashMap<>();
                if (tokens.length > 1) args.put("arg1", tokens[1]);

                if (Set.of("add", "add_if_max", "add_if_min", "update").contains(cmd)) {
                    addHistory(i18n.getString("script.warning.gui_required") + " " + cmd);
                    continue;
                }
                if (cmd.equals("execute_script")) {
                    addHistory(i18n.getString("script.warning.recursion") + " " + line);
                    continue;
                }

                CommandRequest request = new CommandRequest(cmd, args.isEmpty() ? null : args,
                        client.getCurrentLogin(), client.getCurrentPassword());
                CommandResponse resp = client.sendRequest(request);
                addHistory("> " + line);
                addHistory(localizeServerResponse(resp.getString()));
                if (isModifyingCommand(cmd)) {
                    refreshCallbacks.forEach(Runnable::run);
                }
            }
            addHistory(i18n.getString("script.success"));
        } catch (IOException e) {
            addHistory(i18n.getString("script.error.read") + ": " + e.getMessage());
        } catch (Exception e) {
            addHistory(i18n.getString("error.server") + ": " + e.getMessage());
        }
    }

    private void navigateHistory(int direction) {
        if (historyInputs.isEmpty()) return;

        String currentText = commandField.getText().trim();

        if (direction > 0) {
            if (historyIndex == -1) {
                historyIndex = historyInputs.size() - 1;
                currentText = historyInputs.get(historyIndex);
            }
            int start = (historyViewIndex == -1) ? 0 : historyViewIndex + 1;
            int found = -1;
            String prefix = "> " + currentText;
            for (int i = start; i < historyList.size(); i++) {
                if (historyList.get(i).equals(prefix)) {
                    found = i;
                    break;
                }
            }
            if (found != -1) {
                historyViewIndex = found;
                historyView.getSelectionModel().select(found);
                historyView.scrollTo(found);
                navigating = true;
                commandField.setText(currentText);
                commandField.positionCaret(currentText.length());
                navigating = false;
            } else {
                if (historyIndex > 0) {
                    historyIndex--;
                    currentText = historyInputs.get(historyIndex);
                    found = -1;
                    prefix = "> " + currentText;
                    for (int i = 0; i < historyList.size(); i++) {
                        if (historyList.get(i).equals(prefix)) {
                            found = i;
                            break;
                        }
                    }
                    if (found != -1) {
                        historyViewIndex = found;
                        historyView.getSelectionModel().select(found);
                        historyView.scrollTo(found);
                        navigating = true;
                        commandField.setText(currentText);
                        commandField.positionCaret(currentText.length());
                        navigating = false;
                    }
                }
            }
        } else {
            if (historyIndex == -1) return;

            int start = (historyViewIndex == -1) ? historyList.size() : historyViewIndex - 1;
            int found = -1;
            String prefix = "> " + currentText;
            for (int i = start; i >= 0; i--) {
                if (historyList.get(i).equals(prefix)) {
                    found = i;
                    break;
                }
            }
            if (found != -1) {
                historyViewIndex = found;
                historyView.getSelectionModel().select(found);
                historyView.scrollTo(found);
            } else {
                if (historyIndex < historyInputs.size() - 1) {
                    historyIndex++;
                    currentText = historyInputs.get(historyIndex);
                    prefix = "> " + currentText;
                    found = -1;
                    for (int i = historyList.size() - 1; i >= 0; i--) {
                        if (historyList.get(i).equals(prefix)) {
                            found = i;
                            break;
                        }
                    }
                    if (found != -1) {
                        historyViewIndex = found;
                        historyView.getSelectionModel().select(found);
                        historyView.scrollTo(found);
                        navigating = true;
                        commandField.setText(currentText);
                        commandField.positionCaret(currentText.length());
                        navigating = false;
                    } else {
                        historyIndex = -1;
                        historyViewIndex = -1;
                        commandField.clear();
                        historyView.getSelectionModel().clearSelection();
                    }
                } else {
                    historyIndex = -1;
                    historyViewIndex = -1;
                    commandField.clear();
                    historyView.getSelectionModel().clearSelection();
                }
            }
        }
    }
}