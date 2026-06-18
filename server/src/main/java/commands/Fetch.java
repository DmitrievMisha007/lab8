package commands;

import core.*;
import interfases.Command;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fetch implements Command {
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        List<Map<String, Object>> ticketList = new ArrayList<>();
        for (Ticket t : manager.getSnapshot()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("x", t.getCoordinates().getX()); // Double or null
            map.put("y", t.getCoordinates().getY());
            // Дату сериализуем как строку в ISO-формате (или как long)
            map.put("creationDate", t.getCreationDate().getTime()); // миллисекунды
            map.put("price", t.getPrice());
            map.put("comment", t.getComment());
            map.put("refundable", t.isRefundable());
            map.put("type", t.getType() != null ? t.getType().name() : null);
            Event e = t.getEvent();
            if (e != null) {
                map.put("eventName", e.getName());
                map.put("ticketsCount", e.getTicketsCount());
                map.put("eventType", e.getEventType().name());
            } else {
                map.put("eventName", null);
                map.put("ticketsCount", 0L);
                map.put("eventType", null);
            }
            map.put("userId", t.getUserId());
            ticketList.add(map);
        }
        return new CommandResponse("OK", ticketList);
    }
}