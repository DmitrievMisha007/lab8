package client.gui.pages;

import client.NetworkClient;
import client.gui.components.TicketDialog;
import client.i18n.I18nManager;
import core.CommandRequest;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TablePage extends VBox {
    private final NetworkClient client;
    private final Runnable onDataChanged;
    private final TableView<Map<String, Object>> tableView = new TableView<>();
    private final ObservableList<Map<String, Object>> data = FXCollections.observableArrayList();
    private List<Map<String, Object>> fullList = new ArrayList<>();
    private final TextField filterField = new TextField();
    private String currentSortField = null;
    private boolean ascending = true;
    private final Map<String, Label> headerLabels = new LinkedHashMap<>();
    private final I18nManager i18n = I18nManager.getInstance();
    private final ContextMenu contextMenu;

    private static final Map<String, Function<Map<String, Object>, Comparable>> EXTRACTORS = new HashMap<>();
    static {
        EXTRACTORS.put("id", m -> (Long) m.get("id"));
        EXTRACTORS.put("name", m -> (String) m.get("name"));
        EXTRACTORS.put("x", m -> (Double) m.get("x"));
        EXTRACTORS.put("y", m -> (Double) m.get("y"));
        EXTRACTORS.put("creationDate", m -> (Long) m.get("creationDate"));
        EXTRACTORS.put("price", m -> (Double) m.get("price"));
        EXTRACTORS.put("comment", m -> (String) m.get("comment"));
        EXTRACTORS.put("refundable", m -> Boolean.valueOf(m.get("refundable").toString()));
        EXTRACTORS.put("type", m -> (String) m.get("type"));
        EXTRACTORS.put("ticketsCount", m -> (Long) m.get("ticketsCount"));
        EXTRACTORS.put("eventType", m -> (String) m.get("eventType"));
        EXTRACTORS.put("eventName", m -> (String) m.get("eventName"));
        EXTRACTORS.put("userId", m -> (Integer) m.get("userId"));
    }

    public TablePage(NetworkClient client, Runnable onDataChanged) {
        this.client = client;
        this.onDataChanged = onDataChanged;

        filterField.setUserData("label.filter");
        filterField.setPromptText(I18nManager.getInstance().getString("label.filter"));
        filterField.textProperty().addListener((obs, old, newValue) -> applyFilterAndSort());

        TableColumn<Map<String, Object>, String> idCol = createColumn("id");
        TableColumn<Map<String, Object>, String> nameCol = createColumn("name");
        TableColumn<Map<String, Object>, String> xCol = createColumn("x");
        TableColumn<Map<String, Object>, String> yCol = createColumn("y");
        TableColumn<Map<String, Object>, String> dateCol = createColumn("creationDate");
        TableColumn<Map<String, Object>, String> priceCol = createColumn("price");
        TableColumn<Map<String, Object>, String> commentCol = createColumn("comment");
        TableColumn<Map<String, Object>, String> refundCol = createColumn("refundable");
        TableColumn<Map<String, Object>, String> typeCol = createColumn("type");
        TableColumn<Map<String, Object>, String> eventTicketsCol = createColumn("ticketsCount");
        TableColumn<Map<String, Object>, String> eventTypeCol = createColumn("eventType");
        TableColumn<Map<String, Object>, String> eventNameCol = createColumn("eventName");
        TableColumn<Map<String, Object>, String> userIdCol = createColumn("userId");

        tableView.getColumns().addAll(idCol, nameCol, xCol, yCol, dateCol, priceCol,
                commentCol, refundCol, typeCol, eventTicketsCol, eventTypeCol, eventNameCol, userIdCol);
        tableView.setItems(data);

        // Создаём меню один раз
        contextMenu = new ContextMenu();
        contextMenu.setMinWidth(150);
        MenuItem editItem = new MenuItem("Редактировать");
        editItem.setUserData("button.edit");
        MenuItem deleteItem = new MenuItem("Удалить");
        deleteItem.setUserData("button.delete");

        editItem.setOnAction(e -> {
            Map<String, Object> selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) editTicket(selected);
        });
        deleteItem.setOnAction(e -> {
            Map<String, Object> selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) deleteTicket(selected);
        });

        contextMenu.getItems().addAll(editItem, deleteItem);

// Устанавливаем меню через обработчик запроса контекстного меню
        tableView.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                // Выделяем строку, на которой произошёл правый клик
                row.getTableView().getSelectionModel().select(row.getIndex());
                // Показываем меню, только если объект принадлежит текущему пользователю
                Map<String, Object> item = row.getItem();
                if (item != null && (Integer) item.get("userId") == client.getCurrentUserId()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            });
            return row;
        });

// Применяем начальную локализацию
        updateContextMenuText();

        getChildren().addAll(filterField, tableView);
        updateHeaderArrows();
    }

    private TableColumn<Map<String, Object>, String> createColumn(String key) {
        TableColumn<Map<String, Object>, String> col = new TableColumn<>();
        col.setSortable(false);

        String resourceKey = "column." + key;
        Label headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold;");
        headerLabel.setUserData(resourceKey);
        headerLabels.put(key, headerLabel);

        headerLabel.setOnMouseClicked(e -> {
            if (currentSortField != null && currentSortField.equals(key)) {
                if (!ascending) {
                    currentSortField = null;
                    ascending = true;
                } else {
                    ascending = false;
                }
            } else {
                currentSortField = key;
                ascending = true;
            }
            applyFilterAndSort();
            updateHeaderArrows();
        });

        col.setGraphic(headerLabel);
        col.setText(null);

        col.setCellValueFactory(cell -> {
            Object val = cell.getValue().get(key);
            if (val == null) return new javafx.beans.property.SimpleStringProperty("");
            if (key.equals("creationDate") && val instanceof Long millis) {
                DateTimeFormatter fmt = I18nManager.getInstance().getDateTimeFormatter();
                LocalDateTime ldt = new java.util.Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return new javafx.beans.property.SimpleStringProperty(fmt.format(ldt));
            }
            return new javafx.beans.property.SimpleStringProperty(val.toString());
        });

        return col;
    }

    public void updateHeaderArrows() {
        for (Map.Entry<String, Label> entry : headerLabels.entrySet()) {
            String key = entry.getKey();
            Label label = entry.getValue();
            String baseText = i18n.getString((String) label.getUserData());
            if (key.equals(currentSortField)) {
                label.setText(baseText + (ascending ? " ▲" : " ▼"));
            } else {
                label.setText(baseText);
            }
        }
    }

    private void applyFilterAndSort() {
        String filterText = filterField.getText().trim().toLowerCase();
        Stream<Map<String, Object>> stream = fullList.stream();

        if (!filterText.isEmpty()) {
            stream = stream.filter(item ->
                    item.values().stream().anyMatch(val ->
                            val != null && val.toString().toLowerCase().contains(filterText)
                    )
            );
        }

        if (currentSortField != null) {
            Function<Map<String, Object>, Comparable> extractor = EXTRACTORS.get(currentSortField);
            if (extractor != null) {
                Comparator<Map<String, Object>> comparator =
                        Comparator.comparing(extractor, Comparator.nullsLast(Comparator.naturalOrder()));
                if (!ascending) comparator = comparator.reversed();
                stream = stream.sorted(comparator);
            }
        }

        List<Map<String, Object>> filteredSorted = stream.collect(Collectors.toList());
        data.setAll(filteredSorted);
    }

    public void refresh(List<Map<String, Object>> list) {
        fullList = new ArrayList<>(list);
        applyFilterAndSort();
    }

    private void editTicket(Map<String, Object> item) {
        TicketDialog dialog = new TicketDialog(item);
        dialog.showAndWait().ifPresent(newData -> {
            Map<String, Object> args = new LinkedHashMap<>(newData);
            args.put("arg1", item.get("id").toString());
            CommandRequest request = new CommandRequest("update", args, client.getCurrentLogin(), client.getCurrentPassword());
            new Thread(() -> {
                try {
                    client.sendRequest(request);
                    Platform.runLater(onDataChanged);
                } catch (Exception e) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, i18n.getString("error.update") + " " + e.getMessage()).show());
                }
            }).start();
        });
    }

    private void deleteTicket(Map<String, Object> item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, i18n.getString("confirm.delete") + item.get("id") + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Map<String, Object> args = Map.of("arg1", item.get("id").toString());
                CommandRequest request = new CommandRequest("remove_by_id", args, client.getCurrentLogin(), client.getCurrentPassword());
                new Thread(() -> {
                    try {
                        client.sendRequest(request);
                        Platform.runLater(onDataChanged);
                    } catch (Exception e) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, i18n.getString("error.delete") + " " + e.getMessage()).show());
                    }
                }).start();
            }
        });
    }


    public void updateContextMenuText() {
        I18nManager i18n = I18nManager.getInstance();
        for (MenuItem item : contextMenu.getItems()) {
            Object key = item.getUserData();
            if (key instanceof String) {
                item.setText(i18n.getString((String) key));
            }
        }
    }
}