package commands;

import core.Manager;
import core.CommandResponse;
import core.Ticket;
import interfases.Command;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/**
 * Команда, которая выводит все элементы, отсортированные по убыванию цены.
 */
public class PrintFieldDescendingRefundable implements Command {
    /**
     * Вызывает команду
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId){
        if (args != null) return new CommandResponse("Команда не принимает аргументов");
        StringBuilder result = new StringBuilder();
        Iterator<Ticket> iterator = manager.getSnapshot().stream().sorted(Comparator.reverseOrder()).iterator();
        while (iterator.hasNext()){
            result.append(iterator.next().isRefundable()).append("\n");
        }
        return new CommandResponse(result.toString());
    }
}
