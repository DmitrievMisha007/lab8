package commands;

import core.*;
import interfases.Command;

import java.sql.SQLException;
import java.util.Map;

/**
 * Команда добавления нового элемента в коллекцию.
 */
public class Add implements Command {
    /**
     * Вызывает команду.
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId){
        if (args == null) return new CommandResponse("Команда не принимает аргументов");
        Ticket ticket = new Ticket();
        ticket.fromRequest(args);
        try {
            manager.getDbManager().addTicket(ticket, userId);
            manager.addTicketDirectly(ticket);
            return new CommandResponse("Элемент успешно добавлен (id=" + ticket.getId() + ")");
        } catch (SQLException e) {
            return new CommandResponse("Ошибка добавления элемента в БД: "+e.getMessage());
        }
    }
}
