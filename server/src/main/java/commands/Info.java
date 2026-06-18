package commands;

import core.Manager;
import core.CommandResponse;
import interfases.Command;

import java.util.Map;

/**
 * Команда отображения общей информации о коллекции.
 */
public class Info implements Command {
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
        return new CommandResponse(manager.info());
    }
}
