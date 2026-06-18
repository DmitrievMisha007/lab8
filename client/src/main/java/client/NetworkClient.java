//package core;
//
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.SocketChannel;
//import java.nio.file.AccessDeniedException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//
///**
// * Клиентская часть приложения. Устанавливает соединение с сервером,
// * запрашивает у пользователя логин/пароль и в цикле обрабатывает ввод команд,
// * отправляя их на сервер и выводя ответы.
// * Поддерживает выполнение скриптов через команду {@code execute_script}.
// *
// * @see CommandRequest
// * @see CommandResponse
// */
//public class NetworkClient {
//    private final String host;
//    private final int port;
//    private SocketChannel channel;
//    private String login;
//    private String password;
//
//    private boolean connected = false;
//
//
//    private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
//    private ByteBuffer dataBuffer = null;
//    private int expectedLength = -1;
//
//
//    private final Stack<String> scriptStack = new Stack<>();
//    private BufferedReader console;
//
//    public NetworkClient(String host, int port){
//        this.host = host;
//        this.port = port;
//    }
//
//    /**
//     * Запускает клиент: подключается к серверу, запрашивает учётные данные и
//     * переходит в цикл обработки команд.
//     */
//    public void start(){
//        connectWithRetry();
//        System.out.println("Connected to server " + host + ":" + port);
//
//
//        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
//            this.console = console;
//            System.out.print("Введите логин: ");
//            login = console.readLine();
//            System.out.print("Введите пароль: ");
//            password = console.readLine();
//            if (login == null || password == null) {
//                System.out.println("Ввод отменён.");
//                return;
//            }
//            System.out.print("Введите команду: ");
//
//            String line;
//            while ((line = console.readLine()) != null) {
//
//                if (line.trim().startsWith("execute_script")) {
//                    String[] parts = line.trim().split("\\s+");
//                    if (parts.length == 2) {
//                        executeScript(parts[1]);
//                    } else {
//                        System.out.println("Использование: execute_script <имя_файла>");
//                    }
//                    System.out.print("Введите команду: ");
//                    continue;
//                }
//
//                if (line.trim().equalsIgnoreCase("exit")) break;
//                if (line.trim().isEmpty()) {
//                    System.out.println("Ошибка ввода команды!");
//                    System.out.print("Введите команду: ");
//                    continue;
//                }
//
//                if (line.trim().equalsIgnoreCase("login")) {
//                    System.out.print("Введите новый логин: ");
//                    String newLogin = console.readLine();
//                    System.out.print("Введите новый пароль: ");
//                    String newPassword = console.readLine();
//                    this.login = newLogin;
//                    this.password = newPassword;
//                    System.out.println("Данные для входа обновлены.");
//                    System.out.print("Введите команду: ");
//                    continue;
//                }
//
//                CommandRequest request = parseUserInput(line, console);
//
//                try {
//                    CommandResponse response = sendAndReceive(request);
//                    if (response != null) {
//                        System.out.println(response.getString());
//
//                    } else {
//                        System.out.println("Не удалось получить ответ");
//                    }
//                    System.out.print("Введите команду: ");
//                } catch (IOException e) {
//                    System.err.println("Ошибка при обмене: " + e.getMessage());
//                    System.out.println("Попытка переподключения...");
//                    reconnect();
//                    System.out.print("Введите команду: ");
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            closeConnection();
//        }
//    }
//
//
//    /**
//     * Подключение к серверу с повторными попытками.
//     * Неблокирующий режим, таймаут подключения.
//     */
//    private void connectWithRetry() {
//        int maxRetries = 5;
//        int delay = 1000;
//        for (int attempt = 1; attempt <= maxRetries; attempt++) {
//            try {
//                channel = SocketChannel.open();
//                channel.configureBlocking(false);
//                channel.connect(new InetSocketAddress(host, port));
//
//                long timeout = 3000;
//                long start = System.currentTimeMillis();
//                while (!channel.finishConnect()) {
//                    if (System.currentTimeMillis() - start > timeout) {
//                        throw new IOException("Connection timeout");
//                    }
//                    Thread.sleep(10);
//                }
//                connected = true;
//                System.out.println("Соединение установлено");
//                return;
//            } catch (Exception e) {
//                System.err.println("Попытка подключения " + attempt + " не удалась: " + e.getMessage());
//                if (attempt == maxRetries) {
//                    throw new RuntimeException("Не удалось подключиться к серверу после " + maxRetries + " попыток", e);
//                }
//                try {
//                    Thread.sleep(delay);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("Прервано ожидание переподключения", ie);
//                }
//                delay *= 2;
//            }
//        }
//    }
//
//    /**
//     * Сериализация объекта в массив байтов через Java Object Serialization.
//     */
//    private byte[] serialize(Object obj) throws IOException {
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
//             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//            oos.writeObject(obj);
//            return baos.toByteArray();
//        }
//    }
//
//    /**
//     * Десериализация байтов в CommandResponse.
//     */
//    private CommandResponse deserialize(byte[] data) throws IOException {
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
//             ObjectInputStream ois = new ObjectInputStream(bais)) {
//            return (CommandResponse) ois.readObject();
//        } catch (ClassNotFoundException e) {
//            throw new IOException("Не удалось распознать ответ сервера", e);
//        }
//    }
//
//    private void closeConnection() {
//        try {
//            if (channel != null) {
//                channel.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        connected = false;
//    }
//
//    /**
//     * Переподключение после разрыва соединения.
//     */
//    private void reconnect() {
//        closeConnection();
//        connectWithRetry();
//        lengthBuffer.clear();
//        dataBuffer = null;
//        expectedLength = -1;
//    }
//
//    /**
//     * Отправляет запрос на сервер и ожидает ответ.
//     * @param request объект {@link CommandRequest}
//     * @return ответ {@link CommandResponse}
//     * @throws IOException при проблемах сетевого обмена
//     * @throws InterruptedException при прерывании потока ожидания
//     */
//    private CommandResponse sendAndReceive(CommandRequest request) throws IOException, InterruptedException {
//        if (!connected || channel == null) throw new IOException("Not connected");
//
//        byte[] reqData = serialize(request);
//        ByteBuffer lengthBuf = ByteBuffer.allocate(4);
//        lengthBuf.putInt(reqData.length);
//        lengthBuf.flip();
//        ByteBuffer dataBuf = ByteBuffer.wrap(reqData);
//
//        while (lengthBuf.hasRemaining()) {
//            channel.write(lengthBuf);
//        }
//        while (dataBuf.hasRemaining()) {
//            channel.write(dataBuf);
//        }
//
//        while (true) {
//            if (expectedLength == -1) {
//                int read = channel.read(lengthBuffer);
//                if (read == -1) throw new IOException("Сервер закрыл соединение");
//                if (lengthBuffer.remaining() == 0) {
//                    lengthBuffer.flip();
//                    expectedLength = lengthBuffer.getInt();
//                    if (expectedLength <= 0 || expectedLength > 10_000_000) {
//                        throw new IOException("Некорректная длина ответа: " + expectedLength);
//                    }
//                    lengthBuffer.clear();
//                    dataBuffer = ByteBuffer.allocate(expectedLength);
//                } else {
//                    Thread.sleep(10);
//                    continue;
//                }
//            }
//
//            int read = channel.read(dataBuffer);
//            if (read == -1) throw new IOException("Сервер закрыл соединение");
//            if (dataBuffer.remaining() == 0) {
//                dataBuffer.flip();
//                byte[] respData = new byte[dataBuffer.limit()];
//                dataBuffer.get(respData);
//                expectedLength = -1;
//                dataBuffer = null;
//                return deserialize(respData);
//            } else {
//                Thread.sleep(10);
//            }
//        }
//    }
//
//    /**
//     * Разбирает пользовательский ввод и формирует объект запроса.
//     * @param input   строка ввода
//     * @param console BufferedReader для интерактивного заполнения полей
//     * @return готовый {@link CommandRequest}
//     * @throws IOException при ошибках чтения с консоли
//     */
//    private CommandRequest parseUserInput(String input, BufferedReader console) throws IOException {
//        String[] parts = input.strip().split("\\s");
//        Map<String, Object> args = new LinkedHashMap<>();
//        for (int i = 1; i != parts.length; i++){
//            args.put("arg"+i, parts[i]);
//        }
//
//        String commandName = parts[0];
//
//        CommandRequest request =
//        switch (commandName) {
//            case "remove_by_id", "execute_script", "filter_greater_than_price" -> {
//                if (parts.length == 2) yield new CommandRequest(commandName, args, login, password);
//                else yield new CommandRequest(commandName, null, login, password);
//            }
//            case "update" -> {
//                if (parts.length == 2) {
//                    args.putAll(getAddParameters(console));
//                    yield new CommandRequest(commandName, args, login, password);
//                }
//                else yield new CommandRequest(commandName, null, login, password);
//            }
//            case "add", "add_if_max", "add_if_min" -> {
//                if (parts.length > 1) yield new CommandRequest(commandName, null, login, password);
//                else yield new CommandRequest(commandName, getAddParameters(console), login, password);
//            }
//            case "register" -> {
//                if (parts.length > 1) yield new CommandRequest(commandName, null, login, password);
//                else {
//                    System.out.print("Введите новый логин: ");
//                    String loginToSend = console.readLine();
//                    System.out.print("Введите новый пароль: ");
//                    String passwordToSend = console.readLine();
//                    Map<String, Object> regArgs = new LinkedHashMap<>();
//                    regArgs.put("login", loginToSend);
//                    regArgs.put("password", passwordToSend);
//                    yield new CommandRequest(commandName, regArgs, loginToSend, passwordToSend);
//                }
//
//            }
//            default -> {
//                if (parts.length > 1) yield new CommandRequest(commandName, args, login, password);
//                else yield new CommandRequest(commandName, null, login, password);
//            }
//        };
//
//        return request;
//
//    }
//
//    private Map<String, Object> getAddParameters(BufferedReader console) throws IOException {
//        String line;
//        Map<String, Object> result = new LinkedHashMap<>();
//        System.out.print("Введите имя(не может быть пустым): ");
//        while ((line = console.readLine()) != null) {
//            if (!line.trim().isEmpty()) {
//                result.put("name", line);
//                break;
//            }
//            System.out.println("Ошибка: имя не может быть пустым.");
//            System.out.print("Введите имя(не может быть пустым): ");
//        }
//        System.out.print("Введите X <= 851, в качестве десятичного разделителя используется точка: ");
//        while ((line = console.readLine()) != null) {
//
//            try {
//                double x = Double.parseDouble(line.trim());
//                if (x <= 851) {
//                    result.put("x", x);
//                    break;
//                } else {
//                    System.out.println("Ошибка: X должен быть <= 851.");
//                    System.out.print("Введите X <= 851, в качестве десятичного разделителя используется точка: ");
//                }
//            } catch (Exception e) {
//                System.out.println("Ошибка: введите корректное число.");
//                System.out.print("Введите X <= 851, в качестве десятичного разделителя используется точка: ");
//            }
//        }
//        System.out.print("Введите Y <= 621, в качестве десятичного разделителя используется точка: ");
//        while ((line = console.readLine()) != null) {
//
//            try {
//                double y = Double.parseDouble(line.trim());
//                if (y <= 621) {
//                    result.put("y", y);
//                    break;
//                } else {
//                    System.out.println("Ошибка: Y должен быть <= 621.");
//                    System.out.print("Введите Y <= 621, в качестве десятичного разделителя используется точка: ");
//                }
//            } catch (Exception e) {
//                System.out.println("Ошибка: введите корректное число.");
//                System.out.print("Введите Y <= 621, в качестве десятичного разделителя используется точка: ");
//            }
//        }
//        System.out.print("Введите цену > 0, в качестве десятичного разделителя используется точка: ");
//        while ((line = console.readLine()) != null) {
//
//            try {
//                double price = Double.parseDouble(line.trim());
//                if (price > 0) {
//                    result.put("price", price);
//                    break;
//                } else {
//                    System.out.println("Ошибка: цена должна быть > 0.");
//                    System.out.print("Введите цену > 0, в качестве десятичного разделителя используется точка: ");
//                }
//            } catch (Exception e) {
//                System.out.println("Ошибка: введите корректное число.");
//                System.out.print("Введите цену > 0, в качестве десятичного разделителя используется точка: ");
//            }
//        }
//        System.out.print("Введите комментарий, поле не может быть пустым: ");
//        while ((line = console.readLine()) != null) {
//
//            if (!line.trim().isEmpty()) {
//                result.put("comment", line);
//                break;
//            }
//            System.out.println("Ошибка: комментарий не может быть пустым.");
//            System.out.print("Введите комментарий, поле не может быть пустым: ");
//        }
//        System.out.print("Возвратный? (true/false): ");
//        while ((line = console.readLine()) != null) {
//
//            String input = line.trim().toLowerCase();
//            if (input.equals("true") || input.equals("false")) {
//                result.put("refundable", Boolean.parseBoolean(input));
//                break;
//            }
//            System.out.println("Ошибка: введите true или false.");
//            System.out.print("Возвратный? (true/false): ");
//        }
//        System.out.print("Тип билета (USUAL, BUDGETARY, CHEAP) или пусто: ");
//        while ((line = console.readLine()) != null) {
//            String input = line.trim();
//            if (input.isEmpty()) {
//                result.put("type", null);
//                break;
//            }
//            boolean isBreak = false;
//            switch (input.toUpperCase()) {
//                case "USUAL", "BUDGETARY", "CHEAP" -> {
//                    result.put("type", input.toUpperCase());
//                    isBreak = true;
//                }
//            }
//            if (isBreak) break;
//            System.out.println("Неверный тип, должен быть USUAL, BUDGETARY или CHEAP");
//            System.out.print("Тип билета (USUAL, BUDGETARY, CHEAP) или пусто: ");
//        }
//
//        System.out.print("Создать событие? (yes/no) или пусто: ");
//        String answer = console.readLine().trim().toLowerCase();
//
//        if (answer.equals("yes")) {
//            result.put("event", "yes");
//            System.out.print("Имя события (поле не может быть пустым): ");
//            while ((line = console.readLine()) != null) {
//
//                if (!line.trim().isEmpty()) {
//                    result.put("eventName", line.trim());
//                    break;
//                }
//                System.out.println("Ошибка: имя не может быть пустым.");
//                System.out.print("Имя события (поле не может быть пустым): ");
//            }
//
//            System.out.print("Введите количество билетов (целое число > 0): ");
//            while ((line = console.readLine()) != null) {
//                try {
//                    long count = Long.parseLong(line.trim());
//                    if (count > 0) {
//                        result.put("ticketCount", count);
//                        break;
//                    }
//                    System.out.println("Ошибка: должно быть > 0.");
//                    System.out.print("Введите количество билетов (целое число > 0): ");
//                } catch (Exception e) {
//                    System.out.println("Ошибка: введите целое число.");
//                    System.out.print("Введите количество билетов (целое число > 0): ");
//                }
//            }
//
//            System.out.print("Тип события (E_SPORTS, FOOTBALL, BASKETBALL, OPERA, EXPOSITION)(Не может быть пустым): ");
//            while ((line = console.readLine()) != null) {
//                String input = line.trim();
//                if (input.isEmpty()) {
//                    System.out.println("Тип события не может быть пустым");
//                    System.out.print("Тип события (E_SPORTS, FOOTBALL, BASKETBALL, OPERA, EXPOSITION)(Не может быть пустым): ");
//                    continue;
//                }
//                boolean isBreak = false;
//                switch (input.toUpperCase()) {
//                    case "E_SPORTS", "FOOTBALL", "BASKETBALL", "OPERA", "EXPOSITION" -> {
//                        result.put("eventType", input.toUpperCase());
//                        isBreak = true;
//                    }
//                }
//                if (isBreak) break;
//                System.out.println("Неверный тип, должен быть E_SPORTS, FOOTBALL, BASKETBALL, OPERA или EXPOSITION");
//                System.out.print("Тип события (E_SPORTS, FOOTBALL, BASKETBALL, OPERA, EXPOSITION)(Не может быть пустым): ");
//            }
//
//        } else {
//            result.put("event", null);
//        }
//        return result;
//    }
//
//
//    /**
//     * Выполняет скрипт из указанного файла.
//     * При обнаружении команд add/add_if_max/add_if_min/update запрашивает параметры у пользователя.
//     */
//    private void executeScript(String fileName) {
//        if (scriptStack.contains(fileName)) {
//            System.out.println("Обнаружена рекурсия! Выполнение скрипта " + fileName + " пропущено.");
//            return;
//        }
//        scriptStack.push(fileName);
//        Path path = Paths.get(fileName);
//        try {
//            List<String> lines = Files.readAllLines(path);
//            for (String line : lines) {
//                line = line.trim();
//                if (line.isEmpty() || line.startsWith("#")) continue;
//
//                String[] tokens = line.split("\\s+");
//                String command = tokens[0];
//                Map<String, Object> args = null;
//
//                boolean needsInteractive = Set.of("add", "add_if_max", "add_if_min", "update").contains(command);
//
//                if (needsInteractive) {
//                    args = new LinkedHashMap<>();
//                    if (command.equals("update") && tokens.length >= 2) {
//                        try {
//                            long id = Long.parseLong(tokens[1]);
//                            args.put("arg1", String.valueOf(id));
//                        } catch (NumberFormatException e) {
//                            System.out.println("Некорректный id в скрипте для update, будет запрошен интерактивно.");
//                        }
//                    }
//                    Map<String, Object> interactiveArgs = getAddParameters(console);
//                    args.putAll(interactiveArgs);
//                } else {
//                    if (tokens.length > 1) {
//                        args = new LinkedHashMap<>();
//                        for (int i = 1; i < tokens.length; i++) {
//                            args.put("arg" + i, tokens[i]);
//                        }
//                    }
//                    if (command.equals("execute_script") && tokens.length >= 2) {
//                        executeScript(tokens[1]);
//                        continue;
//                    }
//                }
//
//                CommandRequest request = new CommandRequest(command, args, login, password);
//                try {
//                    CommandResponse response = sendAndReceive(request);
//                    if (response != null) {
//                        System.out.println(response.getString());
//                    }
//                } catch (IOException | InterruptedException e) {
//                    System.err.println("Ошибка при выполнении команды " + command + ": " + e.getMessage());
//                    reconnect();
//                }
//            }
//        } catch (AccessDeniedException e) {
//            System.out.println("Недостаточно прав для чтения файла " + fileName);
//        } catch (IOException e) {
//            System.out.println("Ошибка чтения файла " + fileName + ": " + e.getMessage());
//        } finally {
//            scriptStack.pop();
//        }
//    }
//
//
//}


package client;

import core.CommandRequest;
import core.CommandResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Сетевой клиент для соединения с сервером.
 * Использует блокирующие сокеты и протокол [длина_данных (4 байта)][данные].
 * Не содержит логики парсинга команд или ввода-вывода на консоль.
 */
public class NetworkClient {
    private final String host;
    private final int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private String currentLogin;   // запоминаем логин после входа, для отображения
    private int currentUserId = -1;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Устанавливает соединение с сервером с несколькими попытками.
     * @throws IOException если не удалось подключиться после всех попыток
     */
    public synchronized void connect() throws IOException {
        int maxRetries = 5;
        int delayMs = 1000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 3000); // таймаут подключения 3 сек
                out = socket.getOutputStream();
                in = socket.getInputStream();
                System.out.println("Соединение с сервером установлено.");
                return;
            } catch (IOException e) {
                System.err.println("Попытка подключения " + attempt + " не удалась: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new IOException("Не удалось подключиться к серверу после " + maxRetries + " попыток", e);
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Прервано ожидание переподключения", ie);
                }
                delayMs *= 2;
            }
        }
    }

    /**
     * Отправляет запрос и получает ответ. Метод блокирующий, должен вызываться в фоновом потоке.
     * @param request полностью сформированный запрос (включая логин/пароль)
     * @return ответ сервера
     * @throws IOException если произошла ошибка сети или сериализации
     */
    public synchronized CommandResponse sendRequest(CommandRequest request) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Соединение не установлено");
        }
        // Сериализуем запрос
        byte[] reqData = serialize(request);
        // Отправляем длину и данные
        writeInt(reqData.length);
        out.write(reqData);
        out.flush();

        // Читаем ответ: длину, затем данные
        int length = readInt();
        if (length <= 0 || length > 10_000_000) {
            throw new IOException("Некорректная длина ответа: " + length);
        }
        byte[] respData = readNBytes(length);
        return deserialize(respData);
    }

    /**
     * Сохраняет логин текущего пользователя (после успешной авторизации).
     */
    public void setCurrentLogin(String login) {
        this.currentLogin = login;
    }

    public String getCurrentLogin() {
        return currentLogin;
    }

    /**
     * Закрывает соединение.
     */
    public synchronized void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- приватные методы для работы с сетью ---

    private void writeInt(int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
    }

    private byte[] readNBytes(int len) throws IOException {
        byte[] data = new byte[len];
        int offset = 0;
        while (offset < len) {
            int read = in.read(data, offset, len - offset);
            if (read == -1) throw new EOFException();
            offset += read;
        }
        return data;
    }

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        }
    }

    private CommandResponse deserialize(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (CommandResponse) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Не удалось распознать ответ сервера", e);
        }
    }

    public synchronized boolean testConnection() {
        try {
            CommandRequest ping = new CommandRequest("help", null, "", "");
            sendRequest(ping); // если исключение не вылетело, значит сервер ответил
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private String currentPassword;

    public void setCredentials(String login, String password) {
        this.currentLogin = login;
        this.currentPassword = password;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }
}