//package client.gui.pages;
//
//import javafx.scene.canvas.Canvas;
//import javafx.scene.canvas.GraphicsContext;
//import javafx.scene.input.MouseEvent;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.paint.Color;
//
//import java.util.*;
//
//public class VisualizationPage extends HBox {
//    private final Canvas canvas;
//    private final InfoPanel infoPanel;
//    private List<Map<String, Object>> currentData = new ArrayList<>();
//    private final Map<Long, Color> userColorMap = new HashMap<>();
//    private final Random random = new Random();
//
//    // Параметры масштабирования
//    private double scaleX = 1.0, scaleY = 1.0;
//    private double offsetX = 0, offsetY = 0;
//
//    public VisualizationPage() {
//        canvas = new Canvas(700, 500);   // начальный размер, будет растягиваться
//        infoPanel = new InfoPanel();
//
//        // Компоновка: canvas занимает 70%, infoPanel – 30%
//        HBox.setHgrow(canvas, Priority.ALWAYS);
//        getChildren().addAll(canvas, infoPanel);
//        setWidth(1000);
//        setHeight(500);
//
//        canvas.widthProperty().addListener(ev -> redraw());
//        canvas.heightProperty().addListener(ev -> redraw());
//
//        // Клик по canvas: ищем ближайший объект и показываем инфо
//        canvas.setOnMouseClicked(this::onCanvasClicked);
//    }
//
//    /** Обновить список объектов и перерисовать */
//    public void refresh(List<Map<String, Object>> data) {
//        this.currentData = new ArrayList<>(data);
//        redraw();
//    }
//
//    /** Перерисовка canvas */
//    private void redraw() {
//        GraphicsContext gc = canvas.getGraphicsContext2D();
//        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
//
//        if (currentData.isEmpty()) return;
//
//        // 1. Вычисляем границы координат
//        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
//        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            if (x < minX) minX = x;
//            if (x > maxX) maxX = x;
//            if (y < minY) minY = y;
//            if (y > maxY) maxY = y;
//        }
//        // Небольшой отступ, чтобы объекты не прилипали к краям
//        double pad = 20;
//        double dataWidth = maxX - minX;
//        double dataHeight = maxY - minY;
//        if (dataWidth == 0) dataWidth = 1;
//        if (dataHeight == 0) dataHeight = 1;
//
//        double canvasW = canvas.getWidth() - 2 * pad;
//        double canvasH = canvas.getHeight() - 2 * pad;
//        scaleX = canvasW / dataWidth;
//        scaleY = canvasH / dataHeight;
//        offsetX = pad - minX * scaleX;
//        offsetY = pad - minY * scaleY;
//
//        // 2. Рисуем каждый объект
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            int userId = (Integer) obj.get("userId");
//            Color color = getUserColor(userId);
//
//            double screenX = x * scaleX + offsetX;
//            double screenY = y * scaleY + offsetY;
//
//            gc.setFill(color);
//            gc.fillOval(screenX - 5, screenY - 5, 10, 10);  // круг радиусом 5
//        }
//    }
//
//    /** Генерация цвета на основе userId */
//    private Color getUserColor(int userId) {
//        return userColorMap.computeIfAbsent((long) userId, k -> {
//            // генерируем случайный насыщенный цвет
//            float hue = random.nextFloat() * 360;
//            return Color.hsb(hue, 0.8, 0.9);
//        });
//    }
//
//    /** Обработка клика: ищем ближайший объект в радиусе 15 пикселей */
//    private void onCanvasClicked(MouseEvent event) {
//        double clickX = event.getX();
//        double clickY = event.getY();
//        Map<String, Object> closest = null;
//        double minDist = 15; // порог
//
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            double sx = x * scaleX + offsetX;
//            double sy = y * scaleY + offsetY;
//            double dist = Math.hypot(sx - clickX, sy - clickY);
//            if (dist < minDist) {
//                minDist = dist;
//                closest = obj;
//            }
//        }
//
//        if (closest != null) {
//            infoPanel.showInfo(closest);
//        } else {
//            infoPanel.clear();
//        }
//    }
//}

//package client.gui.pages;
//
//import client.NetworkClient;
//import client.gui.components.TicketDialog;
//import core.CommandRequest;
//import javafx.application.Platform;
//import javafx.scene.canvas.Canvas;
//import javafx.scene.canvas.GraphicsContext;
//import javafx.scene.control.*;
//import javafx.scene.input.MouseEvent;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.paint.Color;
//
//import java.util.*;
//
//public class VisualizationPage extends HBox {
//    private final NetworkClient client;
//    private final Canvas canvas;
//    private final InfoPanel infoPanel;
//    private List<Map<String, Object>> currentData = new ArrayList<>();
//    private final Map<Long, Color> userColorMap = new HashMap<>();
//    private final Random random = new Random();
//    private final Runnable onDataChanged;
//
//    // Параметры масштабирования
//    private double scaleX = 1.0, scaleY = 1.0;
//    private double offsetX = 0, offsetY = 0;
//    private Map<String, Object> selectedObject = null; // текущий выбранный объект
//
//    public VisualizationPage(NetworkClient client, Runnable onDataChanged) {
//        this.client = client;
//        this.onDataChanged = onDataChanged;
//        canvas = new Canvas(700, 500);
//        infoPanel = new InfoPanel();
//
//        // Реакция на кнопки в InfoPanel
//        infoPanel.getEditButton().setOnAction(e -> {
//            if (selectedObject != null && (Integer) selectedObject.get("userId") == client.getCurrentUserId()) {
//                editTicket(selectedObject);
//            }
//        });
//        infoPanel.getDeleteButton().setOnAction(e -> {
//            if (selectedObject != null && (Integer) selectedObject.get("userId") == client.getCurrentUserId()) {
//                deleteTicket(selectedObject);
//            }
//        });
//
//        HBox.setHgrow(canvas, Priority.ALWAYS);
//        getChildren().addAll(canvas, infoPanel);
//
//        canvas.widthProperty().addListener(ev -> redraw());
//        canvas.heightProperty().addListener(ev -> redraw());
//        canvas.setOnMouseClicked(this::onCanvasClicked);
//    }
//
//    public void refresh(List<Map<String, Object>> data) {
//        this.currentData = new ArrayList<>(data);
//        // Если выбранный объект больше не существует, сбрасываем выделение
//        if (selectedObject != null && currentData.stream().noneMatch(obj -> obj.get("id").equals(selectedObject.get("id")))) {
//            selectedObject = null;
//            infoPanel.clear();
//            infoPanel.setOwnObject(false);
//        }
//        redraw();
//    }
//
//    private void redraw() {
//        GraphicsContext gc = canvas.getGraphicsContext2D();
//        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
//
//        if (currentData.isEmpty()) return;
//
//        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
//        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            if (x < minX) minX = x;
//            if (x > maxX) maxX = x;
//            if (y < minY) minY = y;
//            if (y > maxY) maxY = y;
//        }
//        double pad = 20;
//        double dataWidth = maxX - minX;
//        double dataHeight = maxY - minY;
//        if (dataWidth == 0) dataWidth = 1;
//        if (dataHeight == 0) dataHeight = 1;
//
//        double canvasW = canvas.getWidth() - 2 * pad;
//        double canvasH = canvas.getHeight() - 2 * pad;
//        scaleX = canvasW / dataWidth;
//        scaleY = canvasH / dataHeight;
//        offsetX = pad - minX * scaleX;
//        offsetY = pad - minY * scaleY;
//
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            int userId = (Integer) obj.get("userId");
//            Color color = getUserColor(userId);
//
//            double screenX = x * scaleX + offsetX;
//            double screenY = y * scaleY + offsetY;
//
//            gc.setFill(color);
//            // Если объект выделен, рисуем его немного больше и с рамкой
//            if (selectedObject != null && obj.get("id").equals(selectedObject.get("id"))) {
//                gc.fillOval(screenX - 7, screenY - 7, 14, 14);
//                gc.setStroke(Color.BLACK);
//                gc.strokeOval(screenX - 7, screenY - 7, 14, 14);
//            } else {
//                gc.fillOval(screenX - 5, screenY - 5, 10, 10);
//            }
//        }
//    }
//
//    private Color getUserColor(int userId) {
//        return userColorMap.computeIfAbsent((long) userId, k -> {
//            float hue = random.nextFloat() * 360;
//            return Color.hsb(hue, 0.8, 0.9);
//        });
//    }
//
//    private void onCanvasClicked(MouseEvent event) {
//        double clickX = event.getX();
//        double clickY = event.getY();
//        Map<String, Object> closest = null;
//        double minDist = 15;
//
//        for (Map<String, Object> obj : currentData) {
//            double x = ((Number) obj.get("x")).doubleValue();
//            double y = ((Number) obj.get("y")).doubleValue();
//            double sx = x * scaleX + offsetX;
//            double sy = y * scaleY + offsetY;
//            double dist = Math.hypot(sx - clickX, sy - clickY);
//            if (dist < minDist) {
//                minDist = dist;
//                closest = obj;
//            }
//        }
//
//        if (closest != null) {
//            selectedObject = closest;
//            infoPanel.showInfo(closest);
//            // активируем кнопки только для своих объектов
//            boolean own = (Integer) closest.get("userId") == client.getCurrentUserId();
//            infoPanel.setOwnObject(own);
//            redraw(); // чтобы выделить
//        } else {
//            selectedObject = null;
//            infoPanel.clear();
//            infoPanel.setOwnObject(false);
//            redraw();
//        }
//    }
//
//    private void editTicket(Map<String, Object> item) {
//        TicketDialog dialog = new TicketDialog(item);
//        dialog.showAndWait().ifPresent(newData -> {
//            Map<String, Object> args = new LinkedHashMap<>(newData);
//            args.put("arg1", item.get("id").toString());
//            CommandRequest request = new CommandRequest("update", args, client.getCurrentLogin(), client.getCurrentPassword());
//            new Thread(() -> {
//                try {
//                    client.sendRequest(request);
//                    Platform.runLater(onDataChanged);
//                } catch (Exception e) {
//                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Ошибка: " + e.getMessage()).show());
//                }
//            }).start();
//        });
//    }
//
//    private void deleteTicket(Map<String, Object> item) {
//        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить объект с ID=" + item.get("id") + "?");
//        confirm.showAndWait().ifPresent(btn -> {
//            if (btn == ButtonType.OK) {
//                Map<String, Object> args = Map.of("arg1", item.get("id").toString());
//                CommandRequest request = new CommandRequest("remove_by_id", args, client.getCurrentLogin(), client.getCurrentPassword());
//                new Thread(() -> {
//                    try {
//                        client.sendRequest(request);
//                        Platform.runLater(onDataChanged);
//                    } catch (Exception e) {
//                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Ошибка: " + e.getMessage()).show());
//                    }
//                }).start();
//            }
//        });
//    }
//}

package client.gui.pages;

import client.NetworkClient;
import client.gui.components.TicketDialog;
import client.i18n.I18nManager;
import core.CommandRequest;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.stream.Collectors;

public class VisualizationPage extends HBox {
    private final NetworkClient client;
    private final Runnable onDataChanged;
    private final Canvas canvas;
    private final InfoPanel infoPanel;
    private List<Map<String, Object>> currentData = new ArrayList<>();
    private List<Map<String, Object>> selectedObjects = new ArrayList<>(); // список выделенных

    private final Map<Long, Color> userColorMap = new HashMap<>();
    private final Random random = new Random();

    private double scaleX = 1.0, scaleY = 1.0;
    private double offsetX = 0, offsetY = 0;

    private static final long FALL_DURATION_NANOS = 500_000_000L;
    private final Map<Long, AnimationState> animations = new HashMap<>();
    private AnimationTimer animationTimer;
    private Set<Long> previousIds = new HashSet<>();

    private static final double BASE_WIDTH = 80;
    private static final double BASE_HEIGHT = 30;
    private static final double MAX_WIDTH = 180;
    private static final double MIN_WIDTH = 70;
    private static final double PRICE_SCALE = 0.5;

    private final I18nManager i18n = I18nManager.getInstance();

    public VisualizationPage(NetworkClient client, Runnable onDataChanged) {
        this.client = client;
        this.onDataChanged = onDataChanged;

        canvas = new Canvas(700, 500);
        infoPanel = new InfoPanel();
        infoPanel.setCurrentUserId(client.getCurrentUserId());

        // Кнопки InfoPanel теперь работают с выбранным в списке объектом
        infoPanel.getEditButton().setOnAction(e -> {
            Map<String, Object> selected = infoPanel.getSelectedObject();
            if (selected != null && (Integer) selected.get("userId") == client.getCurrentUserId()) {
                editTicket(selected);
            }
        });
        infoPanel.getDeleteButton().setOnAction(e -> {
            Map<String, Object> selected = infoPanel.getSelectedObject();
            if (selected != null && (Integer) selected.get("userId") == client.getCurrentUserId()) {
                deleteTicket(selected);
            }
        });

        HBox.setHgrow(canvas, Priority.ALWAYS);
        getChildren().addAll(canvas, infoPanel);

        canvas.widthProperty().addListener(ev -> redraw());
        infoPanel.widthProperty().addListener(ev -> redraw());
        canvas.heightProperty().addListener(ev -> redraw());
        canvas.setOnMouseClicked(this::onCanvasClicked);

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                boolean finished = true;
                Iterator<AnimationState> it = animations.values().iterator();
                while (it.hasNext()) {
                    AnimationState state = it.next();
                    double progress = (double)(now - state.startNanoTime) / FALL_DURATION_NANOS;
                    if (progress >= 1.0) {
                        it.remove();
                    } else {
                        finished = false;
                    }
                }
                if (finished) stop();
                redraw();
            }
        };
    }

    public void refresh(List<Map<String, Object>> data) {
        Set<Long> newIds = data.stream().map(obj -> (Long) obj.get("id")).collect(Collectors.toSet());
        for (Map<String, Object> obj : data) {
            Long id = (Long) obj.get("id");
            if (!previousIds.contains(id)) {
                AnimationState state = new AnimationState();
                state.id = id;
                state.startNanoTime = System.nanoTime();
                state.startY = -BASE_HEIGHT;
                animations.put(id, state);
            }
        }
        animations.keySet().removeIf(id -> !newIds.contains(id));

        // Удаляем из выделения исчезнувшие объекты
        selectedObjects.removeIf(obj -> !newIds.contains(obj.get("id")));

        previousIds = newIds;
        this.currentData = new ArrayList<>(data);

        // Обновляем панель, если список выделенных изменился
        infoPanel.showObjects(selectedObjects);

        if (!animations.isEmpty()) {
            animationTimer.start();
        }

        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (currentData.isEmpty()) return;

        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (Map<String, Object> obj : currentData) {
            double x = ((Number) obj.get("x")).doubleValue();
            double y = ((Number) obj.get("y")).doubleValue();
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        double pad = 40;
        double dataWidth = maxX - minX;
        double dataHeight = maxY - minY;
        if (dataWidth == 0) dataWidth = 1;
        if (dataHeight == 0) dataHeight = 1;

        double infoWidth = infoPanel.getWidth();
        double canvasW = canvas.getWidth() - 2 * pad - infoWidth;
        double canvasH = canvas.getHeight() - 2 * pad;
        scaleX = canvasW / dataWidth;
        scaleY = canvasH / dataHeight;
        offsetX = pad - minX * scaleX;
        offsetY = pad - minY * scaleY;

        long now = System.nanoTime();

        // Собираем ID выделенных объектов для быстрой проверки
        Set<Long> selectedIds = selectedObjects.stream().map(obj -> (Long) obj.get("id")).collect(Collectors.toSet());

        for (Map<String, Object> obj : currentData) {
            long id = (Long) obj.get("id");
            double x = ((Number) obj.get("x")).doubleValue();
            double y = ((Number) obj.get("y")).doubleValue();
            int userId = (Integer) obj.get("userId");
            double price = (Double) obj.get("price");
            Color color = getUserColor(userId);

            double centerX = x * scaleX + offsetX;
            double targetCenterY = y * scaleY + offsetY;
            double centerY = targetCenterY;

            double width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, BASE_WIDTH + price * PRICE_SCALE));
            double height = BASE_HEIGHT;

            AnimationState anim = animations.get(id);
            if (anim != null) {
                double progress = (double)(now - anim.startNanoTime) / FALL_DURATION_NANOS;
                if (progress < 1.0) {
                    centerY = anim.startY + (targetCenterY - anim.startY) * progress;
                } else {
                    animations.remove(id);
                }
            }

            double rectX = centerX - width / 2;
            double rectY = centerY - height / 2;

            // --- Рисуем реалистичный билет ---
            gc.save(); // сохраняем состояние графики

// 1. Тень (смещённый полупрозрачный прямоугольник)
            gc.setFill(Color.gray(0.3, 0.4));
            gc.fillRoundRect(rectX + 3, rectY + 3, width, height, 12, 12);

// 2. Основной фон с градиентом
            double hue = color.getHue();
            Color baseColor = Color.hsb(hue, 0.7, 0.85);
            Color lightColor = Color.hsb(hue, 0.3, 1.0);
            javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(
                    rectX, rectY, rectX, rectY + height,
                    false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, lightColor),
                    new javafx.scene.paint.Stop(1, baseColor)
            );
            gc.setFill(gradient);
            gc.fillRoundRect(rectX, rectY, width, height, 12, 12);

// 3. Рамка
            gc.setStroke(baseColor.darker());
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(rectX + 0.75, rectY + 0.75, width - 1.5, height - 1.5, 12, 12);

// 4. Пунктирная линия разрыва (правее центра)
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.2);
            gc.setLineDashes(5, 3);
            double dashX = rectX + width * 0.7;
            gc.strokeLine(dashX, rectY + 5, dashX, rectY + height - 5);
            gc.setLineDashes(null); // сброс штриховки

// 5. Текст билета
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
            String leftText = "ID:" + id;
            String rightText = "$" + String.format("%.1f", price);
            gc.fillText(leftText, rectX + 8, rectY + 18);          // левая часть
            gc.fillText(rightText, dashX + 6, rectY + 18);         // правая часть (после разделителя)

// 6. Перфорация (отверстия) по краям
            int numDots = 6;                               // количество отверстий с каждой стороны
            double dotRadius = 3;
            gc.setFill(Color.GRAY);
            for (int i = 0; i < numDots; i++) {
                double dotY = rectY + (i + 0.5) * (height / numDots);
                gc.fillOval(rectX - dotRadius, dotY - dotRadius, dotRadius * 2, dotRadius * 2);
                gc.fillOval(rectX + width - dotRadius, dotY - dotRadius, dotRadius * 2, dotRadius * 2);
            }

// 7. Выделение, если объект выбран
            if (selectedIds.contains(id)) {
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeRoundRect(rectX - 1, rectY - 1, width + 2, height + 2, 14, 14);
                gc.setLineWidth(1);
            }

            gc.restore();
        }
    }

    private Color getUserColor(int userId) {
        return userColorMap.computeIfAbsent((long) userId, k -> {
            float hue = random.nextFloat() * 360;
            return Color.hsb(hue, 0.7, 0.85);
        });
    }

    private void onCanvasClicked(MouseEvent event) {
        double clickX = event.getX();
        double clickY = event.getY();
        List<Map<String, Object>> hitObjects = new ArrayList<>();

        for (Map<String, Object> obj : currentData) {
            long id = (Long) obj.get("id");
            double x = ((Number) obj.get("x")).doubleValue();
            double y = ((Number) obj.get("y")).doubleValue();
            double price = (Double) obj.get("price");

            double centerX = x * scaleX + offsetX;
            double targetCenterY = y * scaleY + offsetY;
            double centerY = targetCenterY;

            AnimationState anim = animations.get(id);
            if (anim != null) {
                double progress = (double)(System.nanoTime() - anim.startNanoTime) / FALL_DURATION_NANOS;
                if (progress < 1.0) {
                    centerY = anim.startY + (targetCenterY - anim.startY) * progress;
                }
            }

            double width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, BASE_WIDTH + price * PRICE_SCALE));
            double height = BASE_HEIGHT;
            double rectX = centerX - width / 2;
            double rectY = centerY - height / 2;

            if (clickX >= rectX && clickX <= rectX + width && clickY >= rectY && clickY <= rectY + height) {
                hitObjects.add(obj);
            }
        }

        if (!hitObjects.isEmpty()) {
            selectedObjects = hitObjects;
            infoPanel.showObjects(selectedObjects);
            // Установим владельца для выделенного в списке объекта (по умолчанию первого)
            Map<String, Object> first = selectedObjects.get(0);
            boolean own = (Integer) first.get("userId") == client.getCurrentUserId();
        } else {
            selectedObjects.clear();
            infoPanel.clearAll();
        }
        redraw();
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
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Ошибка: " + e.getMessage()).show());
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
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Ошибка: " + e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    private static class AnimationState {
        long id;
        long startNanoTime;
        double startY;
    }
}