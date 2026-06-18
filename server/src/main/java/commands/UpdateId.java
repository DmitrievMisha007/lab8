package commands;

import core.*;
import interfases.Command;

import java.sql.SQLException;
import java.util.*;

public class UpdateId implements Command {
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args == null) return new CommandResponse("Некорректные аргументы");
        try {
            long id = Long.parseLong((String) args.get("arg1"));

            // Находим текущий объект в памяти
            Ticket oldTicket = manager.getSnapshot().stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Элемент с таким id не найден"));

            // Создаём новый объект, копируем неизменяемые поля
            Ticket newTicket = new Ticket();
            newTicket.setId(id);
            newTicket.setCreationDate(oldTicket.getCreationDate());
            newTicket.setUserId(oldTicket.getUserId());

            // Заполняем изменяемые поля из аргументов
            newTicket.fromRequest(args);  // это установит name, coordinates, price, comment, refundable, type, event

            // Обновляем в БД (проверка прав внутри)
            DatabaseManager db = manager.getDbManager();
            db.updateTicket(id, newTicket, userId);

            // Заменяем объект в коллекции атомарно
            manager.replaceTicket(id, newTicket);

            return new CommandResponse("Элемент с id=" + id + " успешно обновлён");
        } catch (NumberFormatException e) {
            return new CommandResponse("Некорректный аргумент");
        } catch (SecurityException e) {
            return new CommandResponse("Недостаточно прав для изменения");
        } catch (SQLException e) {
            return new CommandResponse("Ошибка БД: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new CommandResponse(e.getMessage());
        }
    }
}