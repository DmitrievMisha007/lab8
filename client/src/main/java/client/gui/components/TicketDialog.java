package client.gui.components;

import client.i18n.I18nManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.*;

public class TicketDialog extends Dialog<Map<String, Object>> {
    private TextField nameField = new TextField();
    private TextField xField = new TextField();
    private TextField yField = new TextField();
    private TextField priceField = new TextField();
    private TextField commentField = new TextField();
    private CheckBox refundableBox = new CheckBox();
    private ComboBox<String> typeCombo = new ComboBox<>();
    private CheckBox hasEventBox = new CheckBox();
    private TextField eventNameField = new TextField();
    private TextField eventTicketsField = new TextField();
    private ComboBox<String> eventTypeCombo = new ComboBox<>();
    private I18nManager i18n = I18nManager.getInstance();

    private Map<String, Object> initialData;

    public TicketDialog(Map<String, Object> initialData) {
        this.initialData = initialData;
        setTitle(initialData == null ? i18n.getString("dialog.title.add") : i18n.getString("dialog.title.edit"));
        hasEventBox.setText(i18n.getString("dialog.field.event.add"));
        ButtonType okButtonType = new ButtonType(i18n.getString("button.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(i18n.getString("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Поля
        grid.add(new Label(i18n.getString("dialog.field.name")), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(i18n.getString("dialog.field.x")), 0, 1);
        grid.add(xField, 1, 1);
        grid.add(new Label(i18n.getString("dialog.field.y")), 0, 2);
        grid.add(yField, 1, 2);
        grid.add(new Label(i18n.getString("dialog.field.price")), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label(i18n.getString("dialog.field.comment")), 0, 4);
        grid.add(commentField, 1, 4);
        grid.add(new Label(i18n.getString("dialog.field.refundable")), 0, 5);
        grid.add(refundableBox, 1, 5);

        typeCombo.getItems().addAll(null, "USUAL", "BUDGETARY", "CHEAP");
        typeCombo.setValue(null);
        grid.add(new Label(i18n.getString("dialog.field.type")), 0, 6);
        grid.add(typeCombo, 1, 6);

        hasEventBox.selectedProperty().addListener((obs, old, val) -> {
            eventNameField.setDisable(!val);
            eventTicketsField.setDisable(!val);
            eventTypeCombo.setDisable(!val);
        });
        grid.add(hasEventBox, 0, 7, 2, 1);

        grid.add(new Label(i18n.getString("dialog.field.event.name")), 0, 8);
        grid.add(eventNameField, 1, 8);
        grid.add(new Label(i18n.getString("dialog.field.event.tickets")), 0, 9);
        grid.add(eventTicketsField, 1, 9);
        eventTypeCombo.getItems().addAll("E_SPORTS", "FOOTBALL", "BASKETBALL", "OPERA", "EXPOSITION");
        grid.add(new Label(i18n.getString("dialog.field.event.type")), 0, 10);
        grid.add(eventTypeCombo, 1, 10);

        // Инициализация значений, если редактирование
        if (initialData != null) {
            nameField.setText((String) initialData.get("name"));
            xField.setText(String.valueOf(initialData.get("x")));
            yField.setText(String.valueOf(initialData.get("y")));
            priceField.setText(String.valueOf(initialData.get("price")));
            commentField.setText((String) initialData.get("comment"));
            refundableBox.setSelected((Boolean) initialData.get("refundable"));
            typeCombo.setValue((String) initialData.get("type"));

            String eventName = (String) initialData.get("eventName");
            if (eventName != null && !eventName.isEmpty()) {
                hasEventBox.setSelected(true);
                eventNameField.setText(eventName);
                eventTicketsField.setText(String.valueOf(initialData.get("ticketsCount")));
                eventTypeCombo.setValue((String) initialData.get("eventType"));
            } else {
                hasEventBox.setSelected(false);
                eventNameField.setDisable(true);
                eventTicketsField.setDisable(true);
                eventTypeCombo.setDisable(true);
            }
        }

        getDialogPane().setContent(grid);

        // Конвертация результата в Map при нажатии OK
        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return validateAndBuild();
            }
            return null;
        });
    }

    private Map<String, Object> validateAndBuild() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError(i18n.getString("validation.name.empty"));
            return null;
        }
        double x, y, price;
        try {
            x = Double.parseDouble(xField.getText().trim());
            if (x > 851) throw new NumberFormatException("X <= 851");
        } catch (NumberFormatException e) {
            showError(i18n.getString("validation.x.invalid"));
            return null;
        }
        try {
            y = Double.parseDouble(yField.getText().trim());
            if (y > 621) throw new NumberFormatException("Y <= 621");
        } catch (NumberFormatException e) {
            showError(i18n.getString("validation.y.invalid"));
            return null;
        }
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) throw new NumberFormatException("Цена > 0");
        } catch (NumberFormatException e) {
            showError(i18n.getString("validation.price.invalid"));
            return null;
        }
        String comment = commentField.getText().trim();
        if (comment.isEmpty()) {
            showError(i18n.getString("validation.comment.empty"));
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("x", x);
        map.put("y", y);
        map.put("price", price);
        map.put("comment", comment);
        map.put("refundable", refundableBox.isSelected());
        map.put("type", typeCombo.getValue());

        if (hasEventBox.isSelected()) {
            // важно: ключ "event" со значением "yes" должен идти перед eventName/ticketCount/eventType
            map.put("event", "yes");
            String evName = eventNameField.getText().trim();
            if (evName.isEmpty()) {
                showError(i18n.getString("validation.event.name.empty"));
                return null;
            }
            long ticketsCount;
            try {
                ticketsCount = Long.parseLong(eventTicketsField.getText().trim());
                if (ticketsCount <= 0) throw new NumberFormatException(">0");
            } catch (NumberFormatException e) {
                showError(i18n.getString("validation.event.tickets.invalid"));
                return null;
            }
            String evType = eventTypeCombo.getValue();
            if (evType == null) {
                showError(i18n.getString("validation.event.type.empty"));
                return null;
            }
            map.put("eventName", evName);
            map.put("ticketsCount", ticketsCount);
            map.put("eventType", evType);
        } else {
            map.put("event", null);
        }
        return map;
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
    }
}