package interfases;

import core.Manager;
import core.CommandResponse;

import java.util.Map;


/**
 * Интерфейс команды
 */
public interface Command {
    /**
     * Вызывает команду
     */
    CommandResponse execute(Manager manager, Map<String, Object> args, int userId);
}
