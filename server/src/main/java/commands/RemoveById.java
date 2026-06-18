package commands;

import core.DatabaseManager;
import core.Manager;
import core.CommandResponse;
import interfases.Command;

import java.sql.SQLException;
import java.util.Map;

/**
 * Команда, которая удаляет элемент по id.
 */
public class RemoveById implements Command {
    /**
     * Вызывает команду
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args == null) return new CommandResponse("Некорректные аргументы");
        try {
            long id = Long.parseLong((String) args.get("arg1"));
            DatabaseManager db = manager.getDbManager();
            boolean removed = db.removeTicket(id, userId);
            if (removed) {
                manager.removeTicketById(id);
                return new CommandResponse("Элемент успешно удалён");
            } else {
                return new CommandResponse("Элемент с таким id не найден");
            }
        } catch (NumberFormatException e) {
            return new CommandResponse("Некорректный аргумент");
        } catch (SecurityException e) {
            return new CommandResponse("Недостаточно прав для удаления");
        } catch (SQLException e) {
            return new CommandResponse("Ошибка БД: " + e.getMessage());
        }
    }
}