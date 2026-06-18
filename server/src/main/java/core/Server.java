package core;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.concurrent.*;

/**
 * Главный класс сервера.
 * Принимает входящие TCP-соединения и распределяет обработку запросов
 * по пулам потоков согласно требованию лабораторной работы:
 * чтение запросов — FixedThreadPool, обработка команд — FixedThreadPool,
 * отправка ответа — новый поток ({@link Thread}).
 *
 * @see Manager
 * @see CommandFactory
 * @see Invoker
 */
public class Server {
    private final int port;
    private final Manager manager;
    private final CommandFactory commandFactory;
    private final Invoker invoker;
    private final ExecutorService readPool;
    private final ExecutorService processPool;
    private volatile boolean running = true;

    public Server(int port, Manager manager, CommandFactory commandFactory, Invoker invoker) {
        this.port = port;
        this.manager = manager;
        this.commandFactory = commandFactory;
        this.invoker = invoker;
        this.readPool = Executors.newFixedThreadPool(10);
        this.processPool = Executors.newFixedThreadPool(10);
    }

    /**
     * Запускает сервер. Создаёт ServerSocket и в бесконечном цикле принимает
     * подключения, передавая каждое в {@link #readPool}.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                readPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Останавливает сервер: снимает флаг {@code running} и завершает пулы потоков.
     */
    public void stop() {
        running = false;
        shutdown();
    }

    private void shutdown() {
        readPool.shutdown();
        processPool.shutdown();
    }

    private void handleClient(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            while (running) {
                byte[] lenBytes = new byte[4];
                int read = in.readNBytes(lenBytes, 0, 4);
                if (read < 4) break;
                int length = ByteBuffer.wrap(lenBytes).getInt();
                if (length <= 0 || length > 10_000_000) break;

                byte[] data = new byte[length];
                int offset = 0;
                while (offset < length) {
                    int bytesRead = in.read(data, offset, length - offset);
                    if (bytesRead == -1) throw new EOFException();
                    offset += bytesRead;
                }

                Future<CommandResponse> future = processPool.submit(() -> processRequest(data));
                CommandResponse response = future.get();

                new Thread(() -> sendResponse(out, response)).start();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("Ошибка обработки клиента: " + e.getMessage());
        }
    }

    private CommandResponse processRequest(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            CommandRequest request = (CommandRequest) ois.readObject();

            DatabaseManager db = manager.getDbManager();
            int userId = -1;

            if (!"register".equals(request.getName())) {
                try {
                    userId = db.authenticateUser(request.getLogin(), request.getPassword());
                } catch (SQLException e) {
                    return new CommandResponse("Ошибка аутентификации: " + e.getMessage());
                }
            }

            return commandFactory.executeCommandByRequest(request, manager, invoker, userId);

        } catch (Exception e) {
            return new CommandResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    private void sendResponse(OutputStream out, CommandResponse response) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(response);
            byte[] respData = baos.toByteArray();

            byte[] len = ByteBuffer.allocate(4).putInt(respData.length).array();
            out.write(len);
            out.write(respData);
            out.flush();
        } catch (IOException e) {
            System.err.println("Ошибка отправки ответа: " + e.getMessage());
        }
    }
}