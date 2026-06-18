package commands;

import core.CommandResponse;
import core.DatabaseManager;
import core.Manager;
import interfases.Command;

import java.sql.SQLException;
import java.util.Map;


/**
 * Команда регистрации нового пользователя.
 */
public class Register implements Command {

    /**
     * Вызывает команду
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args == null) return new CommandResponse("Команда не принимает аргументов");
        String login = (String) args.get("login");
        String password = (String) args.get("password");
        if (login == null || password == null) {
            return new CommandResponse("Логин и пароль обязательны");
        }
        DatabaseManager db = manager.getDbManager();
        try {
            int newId = db.registerUser(login, password);
            return new CommandResponse("Регистрация успешна. Ваш ID: " + newId);
        } catch (SQLException e) {
            return new CommandResponse("Ошибка регистрации: " + e.getMessage());
        }
    }
}
