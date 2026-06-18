package core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


/**
 * Класс, описывающий событие для класса Ticket.
 */
public class Event {
    static private Long currentId = 1L;
    private Long id;
    private String name;
    private long ticketsCount;
    private EventType eventType;
    public Event(){
        id = currentId;
        currentId += 1;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTicketsCount(long ticketsCount) {
        this.ticketsCount = ticketsCount;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getName() {
        return name;
    }

    public long getTicketsCount() {
        return ticketsCount;
    }

    public EventType getEventType() {
        return eventType;
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
}
