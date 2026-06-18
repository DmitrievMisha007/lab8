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
    private String currentLogin;
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
                socket.connect(new InetSocketAddress(host, port), 3000);
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
        byte[] reqData = serialize(request);
        writeInt(reqData.length);
        out.write(reqData);
        out.flush();

        int length = readInt();
        if (length <= 0 || length > 10_000_000) {
            throw new IOException("Некорректная длина ответа: " + length);
        }
        byte[] respData = readNBytes(length);
        return deserialize(respData);
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
            sendRequest(ping);
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