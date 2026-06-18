import core.*;

public class Main {
    public static void main(String[] args) {
        String dbHost = "pg";
        int dbPort = 5432;
        String dbName = "studs";
        String dbUser = "";
        String dbPassword = "";
        int appPort = 8047;

        if (args.length >= 2) {
            dbUser = args[0];
            dbPassword = args[1];
            if (args.length >= 3) appPort = Integer.parseInt(args[2]);
        } else {
            System.err.println("Запустите программу в формате: java Main <логин_БД> <пароль_БД> [порт_сервера]");
            System.exit(1);
        }

        DatabaseManager dbManager = new DatabaseManager(dbHost, dbPort, dbName, dbUser, dbPassword);
        try {
            dbManager.connect();
            dbManager.initDatabase();
        } catch (Exception e) {
            System.err.println("Ошибка БД: " + e.getMessage());
            System.exit(1);
        }

        Manager manager = new Manager(dbManager);
        Invoker invoker = new Invoker();
        invoker.init();
        CommandFactory factory = new CommandFactory();

        Server server = new Server(appPort, manager, factory, invoker);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Завершение работы...");
            server.stop();
            dbManager.closeConnection();
        }));

        server.start();
    }
}