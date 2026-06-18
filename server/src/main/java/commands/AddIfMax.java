package commands;

import core.DatabaseManager;
import core.Manager;
import core.CommandResponse;
import core.Ticket;
import interfases.Command;

import java.sql.SQLException;
import java.util.Map;

/**
 * Команда добавления нового элемента в коллекцию. Элемент добавляется, если он больше максимального из коллекции.
 */
public class AddIfMax implements Command {
    /**
     * Вызывает команду.
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args == null) return new CommandResponse("Команда не принимает аргументов");
        Ticket ticket = new Ticket();
        ticket.fromRequest(args);

        Ticket maxTicket = manager.getSnapshot().stream().max(Ticket::compareTo).orElse(null);

        if (maxTicket == null || ticket.compareTo(maxTicket) > 0) {
            try {
                DatabaseManager db = manager.getDbManager();
                db.addTicket(ticket, userId);
                manager.addTicketDirectly(ticket);
                return new CommandResponse("Элемент добавлен (id=" + ticket.getId() + ")");
            } catch (SQLException e) {
                return new CommandResponse("Ошибка БД: " + e.getMessage());
            }
        } else {
            return new CommandResponse("Элемент не добавлен (не максимальный)");
        }
    }
}
