package core;

import java.io.Serializable;

/**
 * Сериализуемый объект ответа, возвращаемый сервером клиенту.
 * Инкапсулирует текстовое сообщение о результате выполнения команды.
 */
public class CommandResponse implements Serializable {
    private final String string;
    private final Object data;

    public CommandResponse(String string) {
        this.string = string;
        this.data = null;
    }

    public CommandResponse(String string, Object data) {
        this.string = string;
        this.data = data;
    }

    public String getString() {
        return string;
    }

    public Object getData() {
        return data;
    }
}
