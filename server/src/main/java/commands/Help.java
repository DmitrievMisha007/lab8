package commands;

import core.Manager;
import core.CommandResponse;
import interfases.Command;

import java.util.Map;

/**
 * Команда выводит список всех команд.
 */
public class Help implements Command {
    /**
     * Запускает команду.
     * @param manager менеджер коллекции
     * @param args    аргументы, описывающие поля билета
     * @param userId  идентификатор создателя
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId){
        if (args != null) return new CommandResponse("Команда не принимает аргументов");
        String info = "help : вывести справку по доступным командам\n" +
                "info : вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)\n" +
                "show : вывести в стандартный поток вывода все элементы коллекции в строковом представлении\n" +
                "add {element} : добавить новый элемент в коллекцию\n" +
                "update id {element} : обновить значение элемента коллекции, id которого равен заданному\n" +
                "remove_by_id id : удалить элемент из коллекции по его id\n" +
                "clear : очистить коллекцию\n" +
                "execute_script file_name : считать и исполнить скрипт из указанного файла. В скрипте содержатся команды в таком же виде, в котором их вводит пользователь в интерактивном режиме.\n" +
                "add_if_max {element} : добавить новый элемент в коллекцию, если его значение превышает значение наибольшего элемента этой коллекции\n" +
                "add_if_min {element} : добавить новый элемент в коллекцию, если его значение меньше, чем у наименьшего элемента этой коллекции\n" +
                "history : вывести последние 10 команд (без их аргументов)\n" +
                "filter_greater_than_price price : вывести элементы, значение поля price которых больше заданного\n" +
                "print_field_ascending_price : вывести значения поля price всех элементов в порядке возрастания\n" +
                "print_field_descending_refundable : вывести значения поля refundable всех элементов в порядке убывания\n" +
                "register : зарегистрировать нового пользователя";
        return new CommandResponse(info);
    }
}
