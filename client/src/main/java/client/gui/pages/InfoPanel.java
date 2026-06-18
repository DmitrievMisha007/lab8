package client.gui.pages;

import client.i18n.I18nManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InfoPanel extends VBox {
    private int currentUserId = -1;

    private final ListView<Map<String, Object>> objectList = new ListView<>();
    private final ObservableList<Map<String, Object>> listItems = FXCollections.observableArrayList();

    private final Label idLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label coordLabel = new Label();
    private final Label dateLabel = new Label();
    private final Label priceLabel = new Label();
    private final Label commentLabel = new Label();
    private final Label refundLabel = new Label();
    private final Label typeLabel = new Label();
    private final Label eventNameLabel = new Label();
    private final Label eventTicketsLabel = new Label();
    private final Label eventTypeLabel = new Label();

    private final Button editButton = new Button();
    private final Button deleteButton = new Button();

    private final I18nManager i18n = I18nManager.getInstance();
    public InfoPanel() {
        setPadding(new Insets(10));
        setSpacing(5);

        objectList.setItems(listItems);
        objectList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("ID:" + item.get("id") + " | " + item.get("name") + " | $" + item.get("price"));
                }
            }
        });

        objectList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDetails(newVal);
            } else {
                clearDetails();
            }
            updateOwnSelected();
        });

        Label selectLabel = new Label();
        selectLabel.setUserData("label.objects");
        Label detailsLabel = new Label();
        detailsLabel.setUserData("label.details");
        editButton.setUserData("button.edit");
        deleteButton.setUserData("button.delete");

        getChildren().addAll(selectLabel, objectList, detailsLabel,
                idLabel, nameLabel, coordLabel, dateLabel, priceLabel,
                commentLabel, refundLabel, typeLabel, eventNameLabel, eventTicketsLabel, eventTypeLabel,
                editButton, deleteButton);

        I18nManager.getInstance().updateAllTexts(this);
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        updateOwnSelected();
    }

    public void showObjects(List<Map<String, Object>> objects) {
        Map<String, Object> currentSel = objectList.getSelectionModel().getSelectedItem();
        Long selectedId = (currentSel != null) ? (Long) currentSel.get("id") : null;

        listItems.setAll(objects);

        if (selectedId != null) {
            for (Map<String, Object> obj : objects) {
                if (selectedId.equals(obj.get("id"))) {
                    objectList.getSelectionModel().select(obj);
                    return;
                }
            }
        }
        if (!objects.isEmpty()) {
            objectList.getSelectionModel().selectFirst();
        }
    }

    private void showDetails(Map<String, Object> ticketMap) {
        idLabel.setText(i18n.getString("info.label.id") + " " + ticketMap.get("id"));
        nameLabel.setText(i18n.getString("info.label.name") + " " + ticketMap.get("name"));
        coordLabel.setText(String.format("%s X=%.2f, Y=%.2f", i18n.getString("info.label.coordinates"),
                ticketMap.get("x"), ticketMap.get("y")));
        Object timeObj = ticketMap.get("creationDate");
        if (timeObj instanceof Long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateLabel.setText(i18n.getString("info.label.creationDate") + " " + sdf.format(new Date(millis)));
        } else {
            dateLabel.setText(i18n.getString("info.label.creationDate") + " " + i18n.getString("info.label.unknown_date"));
        }
        priceLabel.setText(i18n.getString("info.label.price") + " " + ticketMap.get("price"));
        commentLabel.setText(i18n.getString("info.label.comment") + " " + ticketMap.get("comment"));
        refundLabel.setText(i18n.getString("info.label.refundable") + " " + ticketMap.get("refundable"));
        typeLabel.setText(i18n.getString("info.label.type") + " " + ticketMap.get("type"));
        Object evName = ticketMap.get("eventName");
        if (evName != null && !evName.toString().isEmpty()) {
            eventNameLabel.setText(i18n.getString("info.label.event.name") + " " + evName);
            eventTicketsLabel.setText(i18n.getString("info.label.event.tickets") + " " + ticketMap.get("ticketsCount"));
            eventTypeLabel.setText(i18n.getString("info.label.event.type") + " " + ticketMap.get("eventType"));
        } else {
            eventNameLabel.setText(i18n.getString("info.label.no_event"));
            eventTicketsLabel.setText("");
            eventTypeLabel.setText("");
        }
    }

    private void clearDetails() {
        idLabel.setText("");
        nameLabel.setText("");
        coordLabel.setText("");
        dateLabel.setText("");
        priceLabel.setText("");
        commentLabel.setText("");
        refundLabel.setText("");
        typeLabel.setText("");
        eventNameLabel.setText("");
        eventTicketsLabel.setText("");
        eventTypeLabel.setText("");
    }

    public void clearAll() {
        listItems.clear();
        clearDetails();
        editButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void updateOwnSelected() {
        Map<String, Object> sel = objectList.getSelectionModel().getSelectedItem();
        boolean own = sel != null && (Integer) sel.get("userId") == currentUserId;
        editButton.setDisable(!own);
        deleteButton.setDisable(!own);
    }

    public Button getEditButton() {
        return editButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }

    public Map<String, Object> getSelectedObject() {
        return objectList.getSelectionModel().getSelectedItem();
    }
}