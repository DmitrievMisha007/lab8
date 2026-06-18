package core;

import java.io.Serializable;
import java.util.Map;

/**
 * Сериализуемый объект запроса, отправляемый клиентом на сервер.
 * Содержит имя команды, аргументы, а также логин и пароль для аутентификации.
 */
public class CommandRequest implements Serializable {
    private String name;
    private Map<String, Object> args;
    private String login;
    private String password;

    public CommandRequest(String name, Map<String, Object> args, String login, String password){
        this.name = name;
        this.args = args;
        this.login = login;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
