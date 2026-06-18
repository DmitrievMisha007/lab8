package commands;

import core.CommandResponse;
import core.Manager;
import interfases.Command;

import java.util.Map;

public class WhoAmI implements Command {
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        // userId передан сервером после аутентификации
        return new CommandResponse("OK", userId);
    }
}