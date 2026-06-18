package commands;

import core.Manager;
import core.CommandResponse;
import interfases.Command;

import java.util.Map;

/**
 * Команда, которая отображает историю вызова последних 10 команд.
 */
public class History implements Command {
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
        for (String commandName : manager.getHistory()) {
            result.append(commandName).append("\n");
        }
        return new CommandResponse(result.toString());
    }
}
