package core;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Потокобезопасный менеджер коллекции {@link Ticket}.
 * Синхронизация доступа к коллекции осуществляется с помощью
 * {@link ReentrantLock} в соответствии с заданием.
 * Хранит историю выполненных команд и предоставляет методы для безопасного
 * чтения и модификации коллекции.
 *
 * @see Ticket
 * @see DatabaseManager
 */
public class Manager {
    private final Date initDate;
    private final ArrayDeque<Ticket> collection = new ArrayDeque<>();
    private final ArrayDeque<String> history;
    private final ReentrantLock lock = new ReentrantLock();
    private final DatabaseManager dbManager;

    public Manager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initDate = new Date();
        history = new ArrayDeque<>();
        try {
            dbManager.loadCollection(this);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки коллекции из БД: " + e.getMessage());
        }
    }

    /**
     * Возвращает объект {@link DatabaseManager}, ассоциированный с этим менеджером.
     * @return ссылка на {@code DatabaseManager}
     */
    public DatabaseManager getDbManager() { return dbManager; }


    /**
     * Безопасно добавляет билет в коллекцию (под блокировкой).
     * @param ticket добавляемый объект
     */
    public void addTicketDirectly(Ticket ticket) {
        lock.lock();
        try { collection.add(ticket); } finally { lock.unlock(); }
    }


    /**
     * Удаляет билет по идентификатору, если он существует.
     * @param id идентификатор билета
     */
    public void removeTicketById(long id) {
        lock.lock();
        try {
            collection.removeIf(t -> t.getId() == id);
        } finally { lock.unlock(); }
    }


    /**
     * Удаляет все билеты, принадлежащие указанному пользователю.
     * @param userId идентификатор пользователя
     */
    public void removeUserTickets(int userId) {
        lock.lock();
        try { collection.removeIf(t -> t.getUserId() == userId); } finally { lock.unlock(); }
    }


    /**
     * Возвращает потокобезопасный снимок коллекции в виде списка.
     * @return новый {@link ArrayList}, содержащий все билеты коллекции
     */
    public List<Ticket> getSnapshot() {
        lock.lock();
        try { return new ArrayList<>(collection); } finally { lock.unlock(); }
    }


    /**
     * Возвращает историю выполненных команд (последние 10, без аргументов).
     * @return объект {@link ArrayDeque} с именами команд
     */
    public ArrayDeque<String> getHistory() { return history; }


    /**
     * Добавляет имя команды в историю, удаляя старейшую запись при превышении лимита в 10 команд.
     * @param commandName имя выполненной команды
     */
    public void updateHistory(String commandName) {
        history.add(commandName);
        if (history.size() > 10) history.removeFirst();
    }


    /**
     * Возвращает общую информацию о коллекции: тип, дата инициализации, количество элементов.
     * @return строка с информацией
     */
    public String info() {
        lock.lock();
        try {
            return "type: " + collection.getClass().getName() + "\n" +
                    "init date: " + initDate + "\n" +
                    "amount of elements: " + collection.size();
        } finally { lock.unlock(); }
    }

    /**
     * Формирует строковое представление всех элементов коллекции.
     * @return текстовое представление коллекции или сообщение "Коллекция пуста"
     */
    public String getStringToShow() {
        List<Ticket> snapshot = getSnapshot();
        if (snapshot.isEmpty()) return "Коллекция пуста";
        StringBuilder result = new StringBuilder();
        for (Ticket t : snapshot) result.append(t.toString()).append("\n");
        return result.toString();
    }

    /**
     * Заменяет билет с заданным id на новый объект (атомарная операция).
     * Используется при обновлении объекта после успешного выполнения запроса в БД.
     *
     * @param id        идентификатор заменяемого билета
     * @param newTicket новый объект {@link Ticket}
     */
    public void replaceTicket(long id, Ticket newTicket) {
        lock.lock();
        try {
            Iterator<Ticket> it = collection.iterator();
            while (it.hasNext()) {
                Ticket t = it.next();
                if (t.getId() == id) {
                    it.remove();
                    collection.add(newTicket);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

}