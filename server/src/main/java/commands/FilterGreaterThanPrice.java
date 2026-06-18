package commands;

import core.Manager;
import core.CommandResponse;
import core.Ticket;
import interfases.Command;

import java.util.Map;

/**
 * Команда выводит все элементы коллекции, цена которых больше данной.
 */
public class FilterGreaterThanPrice implements Command {
    /**
     * Запускает команду
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId){
        if (args == null) return new CommandResponse("Некорректные аргументы");
        try {
            double price = Double.parseDouble((String) args.get("arg1"));
            StringBuilder result = new StringBuilder();
            for (Ticket t : manager.getSnapshot()) {
                if (t.getPrice() > price) result.append(t.toString()).append("\n");
            }
            return new CommandResponse(result.toString());
        } catch (NumberFormatException e) {
            return new CommandResponse("Некорректный аргумент");
        }
    }
}
