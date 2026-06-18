package core;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Класс, представляющий билет.
 * Содержит все поля, соответствующие таблице {@code tickets}, а также методы
 * для заполнения из аргументов, переданных клиентом.
 *
 * @see Coordinates
 * @see Event
 * @see TicketType
 */
public class Ticket implements Comparable<Ticket>{
    private long id;
    private String name;
    private Coordinates coordinates;
    private Date creationDate;
    private double price;
    private String comment;
    private boolean refundable;
    private TicketType type;
    private Event event;


    private int userId; // новый владелец
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


    public Ticket(){
        creationDate = new Date();
        coordinates = new Coordinates();
    }

    public void setName(String name){
        this.name = name;
    }

    public void setCoordinates(Coordinates coordinates){
        this.coordinates = coordinates;
    }

    public void setPrice(double price){
        this.price = price;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public String getComment() {
        return comment;
    }

    public double getPrice() {
        return price;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public TicketType getType() {
        return type;
    }

    public boolean isRefundable() {   // для boolean используется is
        return refundable;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public String toString(){
        Field[] fields = this.getClass().getDeclaredFields();
        StringBuilder result = new StringBuilder();
        for (Field f : fields){
            try {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Object value = f.get(this);
                if (value != null){
                    result.append(f.getName()).append(": ").append(value).append("\n");
                }
                else {
                    result.append(f.getName()).append(": null\n");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }

    /**
     * Заполняет поля билета из переданной карты аргументов.
     * Используется командами {@code add}, {@code update} и подобными.
     *
     * @param args Map, содержащий пары ключ-значение (имя поля – значение)
     */
    public void fromRequest(Map<String, Object> args) {
        Iterator<Map.Entry<String, Object>> iterator = args.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            switch (entry.getKey()) {
                case "name" -> this.setName((String) entry.getValue());
                case "x" -> coordinates.setX((Double) entry.getValue());
                case "y" -> coordinates.setY((Double) entry.getValue());
                case "price" -> this.setPrice((Double) entry.getValue());
                case "comment" -> this.setComment((String) entry.getValue());
                case "refundable" -> this.setRefundable((Boolean) entry.getValue());
                case "type" -> {
                    if (entry.getValue() == null) this.setType(null);
                    else this.setType(TicketType.valueOf((String) entry.getValue()));
                }
                case "event" -> {
                    if (entry.getValue() == null) this.setEvent(null);
                    else if (entry.getValue().equals("yes")) {
                        Event event = new Event();
                        while (iterator.hasNext()) {
                            entry = iterator.next();
                            switch (entry.getKey()) {
                                case "eventName" -> event.setName((String) entry.getValue());
                                case "ticketsCount" -> event.setTicketsCount((Long) entry.getValue());
                                case "eventType" -> event.setEventType(EventType.valueOf((String) entry.getValue()));
                            }
                        }
                        this.setEvent(event);
                    }
                }
            }
        }
        this.setCoordinates(coordinates);
    }


    /**
     * Сравнивает два билета по координатам (используется {@link Coordinates#compareTo}).
     *
     * @param o билет для сравнения
     * @return 0 при равенстве, положительное или отрицательное число в зависимости от порядка
     */
    @Override
    public int compareTo(Ticket o) {
        return coordinates.compareTo(o.coordinates);
    }

}

