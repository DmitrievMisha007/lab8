package core;
import interfases.Command;

/**
 * Фабрика команд. По имени команды из запроса выбирает соответствующий
 * объект из {@link Invoker} и выполняет его с переданными аргументами и userId.
 *
 * @see Invoker
 * @see Command
 */
public class CommandFactory {
    /**
     * Выполняет команду, идентифицированную по имени в запросе.
     *
     * @param request запрос клиента (имя команды и аргументы)
     * @param manager менеджер коллекции
     * @param invoker хранилище экземпляров команд
     * @param userId  идентификатор пользователя
     * @return ответ {@link CommandResponse}, сформированный командой
     */
    public CommandResponse executeCommandByRequest(CommandRequest request, Manager manager, Invoker invoker, int userId){
        CommandResponse response = switch (request.getName()){
            case "add"-> invoker.add.execute(manager, request.getArgs(), userId); // +
            case "add_if_max" -> invoker.addIfMax.execute(manager, request.getArgs(), userId); // +
            case "add_if_min" -> invoker.addIfMin.execute(manager, request.getArgs(), userId); // +
            case "clear" -> invoker.clear.execute(manager, request.getArgs(), userId); // +
            case "filter_greater_than_price" -> invoker.filterGreaterThanPrice.execute(manager, request.getArgs(), userId); // +
            case "help" -> invoker.help.execute(manager, request.getArgs(), userId); // +
            case "history" -> invoker.history.execute(manager, request.getArgs(), userId); // +
            case "info" -> invoker.info.execute(manager, request.getArgs(), userId); // +
            case "print_field_ascending_price" -> invoker.printFieldAscendingPrice.execute(manager, request.getArgs(), userId); // +
            case "print_field_descending_refundable" -> invoker.printFieldDescendingRefundable.execute(manager, request.getArgs(), userId); // +
            case "remove_by_id" -> invoker.removeById.execute(manager, request.getArgs(), userId); // +
            case "show" -> invoker.show.execute(manager, request.getArgs(), userId); // +
            case "update" -> invoker.updateId.execute(manager, request.getArgs(), userId); // +
            case "register" -> invoker.register.execute(manager, request.getArgs(), userId); // +
            case "fetch" -> invoker.fetch.execute(manager, request.getArgs(), userId);
            case "whoami" -> invoker.whoami.execute(manager, request.getArgs(), userId);
            default -> new CommandResponse("Команда не распознана");
        };
        manager.updateHistory(request.getName());
        return response;
    }
}
