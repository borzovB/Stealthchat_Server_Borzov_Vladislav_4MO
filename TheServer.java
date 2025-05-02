package org.face_recognition;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

// Класс TheServer реализует серверное приложение, использующее защищённое соединение SSL/TLS для обработки клиентских запросов
// Основные функции:
// - Создание и управление защищённым серверным сокетом для взаимодействия с клиентами
// - Реализация ограничений на количество подключений с одного IP-адреса для предотвращения перегрузки
// - Обработка клиентских команд (строковые, массивы строк, передача файлов) для выполнения различных операций, таких как:
// - Аутентификация пользователей, обновление паролей, проверка уникальности данных
// - Управление контактами (добавление, удаление, синхронизация)
// - Отправка и получение сообщений и файлов между клиентами
// - Генерация и управление криптографическими ключами (RSA) для шифрования
// - Взаимодействие с базой данных для хранения и управления информацией о пользователях, контактах, запросах и ответах
// - Асинхронная обработка сообщений и файлов через приоритетные очереди для каждого клиента
// - Логирование ключевых событий, таких как запуск сервера, подключение клиентов и ошибки
// - Обеспечение безопасности через шифрование, управление блокировками IP и проверку доступа

public class TheServer {

    // Конфигурационные параметры сервера
    private static int PORT; // Порт, на котором сервер принимает клиентские подключения
    private static String NAME_KEY; // Название системного свойства для указания IP-адреса сервера
    private static String SERVER_IP; // IP-адрес сервера, используемый для привязки сокета
    private static String NAME_SSL; // Название системного свойства для конфигурации SSL
    private static String PASSWORD_SERVER; // Пароль для настройки SSL-соединения
    private static String PASSWORD; // Пароль для доступа к базе данных или другим защищённым ресурсам

    // Ограничения для защиты от DDoS-атак и перегрузки
    private static final int MAX_CONNECTIONS_PER_IP = 5; // Максимальное количество одновременных подключений с одного IP
    private static final long BLOCK_DURATION_MS = 300_000; // Длительность блокировки IP-адреса в миллисекундах (5 минут)
    private static final long CONNECTION_INTERVAL_MS = 60_000; // Минимальный интервал между подключениями с одного IP (1 минута)

    // Хранилища данных для управления клиентами и их состоянием
    private static final Map<String, Integer> clientConnectionCounts = new HashMap<>(); // Счётчик активных подключений для каждого IP
    private static final Map<String, Long> blockedIPs = new HashMap<>(); // Список заблокированных IP и время окончания блокировки
    private static final Map<String, Long> lastConnectionTime = new HashMap<>(); // Время последнего подключения для каждого IP
    private static final Map<String, ObjectOutputStream> clientStreams = new HashMap<>(); // Потоки вывода для отправки данных клиентам
    private static final Map<String, PriorityBlockingQueue<QueueItem>> clientQueues = new HashMap<>(); // Очереди сообщений и файлов для каждого клиента

    // Компоненты для обеспечения безопасности и работы с базой данных
    private static final ThreadLocal<PrivateKey> threadLocalPrivateKey = new ThreadLocal<>(); // Хранилище приватных ключей RSA для каждого потока
    private static Database database; // Объект для взаимодействия с базой данных (CRUD-операции, проверки, синхронизация)
    private static final Safety safety = new Safety(); // Объект для выполнения операций безопасности, таких как генерация ID и шифрование

    private static SSLServerSocket serverSocket; // SSL-серверный сокет для обработки защищённых клиентских соединений

    public TheServer(int port, String nameKey, String serverIp, String nameSsl, String passwordServer, String password) throws Exception {
        PORT = port;
        NAME_KEY = nameKey;
        SERVER_IP = serverIp;
        NAME_SSL = nameSsl;
        PASSWORD_SERVER = passwordServer;
        PASSWORD = password;
        database = new Database(clientStreams, PASSWORD);// Инициализация базы данных с передачей потоков клиентов и пароля
    }

    // Метод для остановки сервера
    // Закрывает серверный сокет, если он открыт, и освобождает ресурсы
    // Логирует успешное закрытие или ошибки

    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Серверный сокет закрыт.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии серверного сокета: " + e.getMessage());
        }
    }

    // Метод для запуска сервера
    // - Устанавливает системные свойства для IP и SSL
    // - Создаёт SSL-серверный сокет с использованием фабрики SSLServerSocketFactory
    // - Инициализирует базу данных и проверяет наличие необходимых таблиц
    // - Запускает бесконечный цикл для принятия клиентских подключений
    // - Проверяет ограничения на подключения и блокировки IP перед обработкой каждого клиента
    // - Для каждого нового клиента создаёт отдельный поток для обработки запросов

    static void startServer() throws Exception {
        System.setProperty(NAME_KEY, SERVER_IP); // Установка системного свойства для IP-адреса
        System.setProperty(NAME_SSL, PASSWORD_SERVER); // Установка системного свойства для SSL
        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault(); // Получение фабрики SSL-сокетов
        serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT); // Создание серверного сокета на указанном порту

        System.out.println("Сервер запущен на порту " + PORT);
        database.initializeDatabase(); // Инициализация соединения с базой данных

        // Список таблиц, которые должны существовать в базе данных
        List<String> tableNames = List.of(
                "users", // Таблица пользователей
                "contacts", // Таблица контактов
                "scheduling_requests", // Таблица запросов на планирование
                "application_responses", // Таблица ответов на запросы
                "application_requests" // Таблица запросов
        );

        // Проверка наличия всех необходимых таблиц
        boolean allExist = database.areTablesExist(tableNames);
        if (allExist) {
            System.out.println("Все таблицы существуют.");
        } else {
            System.out.println("Не все таблицы найдены.");
        }

        // Бесконечный цикл для обработки входящих подключений
        while (true) {
            SSLSocket sslSocket = (SSLSocket) serverSocket.accept(); // Ожидание нового клиентского подключения
            InetAddress clientAddress = sslSocket.getInetAddress(); // Получение адреса клиента
            String clientIP = clientAddress.getHostAddress(); // Извлечение IP-адреса клиента

            // Проверка, заблокирован ли IP клиента
            if (isBlocked(clientIP)) {
                sslSocket.close(); // Закрытие сокета, если IP заблокирован
                continue;
            }

            // Проверка ограничений на количество подключений с IP
            if (!checkConnectionLimit(clientIP)) {
                blockIP(clientIP); // Блокировка IP при превышении лимита
                sslSocket.close(); // Закрытие сокета
                continue;
            }

            // Запуск обработки клиента в отдельном потоке
            new Thread(() -> handleClient(sslSocket)).start();
        }
    }

    // Проверка заблокирован ли IP-адрес клиента.
    private static boolean isBlocked(String clientIP) {
        Long blockTime = blockedIPs.get(clientIP);
        if (blockTime != null) {
            if (System.currentTimeMillis() > blockTime) {
                blockedIPs.remove(clientIP);
                return false;
            }
            return true;
        }
        return false;
    }

    // Блокирует IP-адрес на заданное время
    // Добавляет IP в список заблокированных с меткой времени окончания блокировки
    private static void blockIP(String clientIP) {
        blockedIPs.put(clientIP, System.currentTimeMillis() + BLOCK_DURATION_MS);
    }

    // Проверяет ограничение на количество подключений с одного IP
    // - Отслеживает время последнего подключения и количество активных соединений
    // - Если интервал между подключениями меньше заданного, увеличивает счётчик
    // - Сбрасывает счётчик, если интервал превышен

    private static boolean checkConnectionLimit(String clientIP) {
        long currentTime = System.currentTimeMillis();
        lastConnectionTime.putIfAbsent(clientIP, currentTime);
        clientConnectionCounts.putIfAbsent(clientIP, 0);

        if (currentTime - lastConnectionTime.get(clientIP) < CONNECTION_INTERVAL_MS) {
            int currentCount = clientConnectionCounts.get(clientIP);
            clientConnectionCounts.put(clientIP, currentCount + 1);
        } else {
            clientConnectionCounts.put(clientIP, 1);
            lastConnectionTime.put(clientIP, currentTime);
        }
        return clientConnectionCounts.get(clientIP) <= MAX_CONNECTIONS_PER_IP;
    }

    // Обрабатывает клиентское соединение в отдельном потоке
    // - Читает команды от клиента через ObjectInputStream
    // - Передаёт команды на обработку в зависимости от их типа (строка, массив строк, файл)
    // - Закрывает соединение при ошибке или завершении сессии

    private static void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Object command;
            // Цикл чтения команд от клиента
            while ((command = in.readObject()) != null) {
                if (command instanceof String strCommand) {
                    processStringCommand(strCommand, out);// Обработка строковой команды
                } else if (command instanceof String[] parts) {
                    processArrayCommand(parts, out);// Обработка команды в виде массива строк
                } else if (command instanceof FileTransferData data) {
                    handleFileTransfer(data);// Обработка передачи файла
                }
            }
        } catch (Exception e) {
            System.out.println("Сессия завершена: " + e.getMessage());
        }
    }

    // Обработчик строковых команд
    private static void processStringCommand(String strCommand, ObjectOutputStream out) throws Exception {
        String[] parts = strCommand.split(" ", 2);// Разделение команды на действие и параметры
        String action = parts[0];// Извлечение действия

        switch (action) {
            case "GET_EMPLOYEES" -> {
                // Получение данных сотрудника из базы данных
                String[] emp = strCommand.split(" ");
                String result = database.sendEmployeeData(emp, "user_name", "user_password", "user_login");
                out.writeObject(result);// Отправка результата клиенту
            }
            // Обновление пароля пользователя
            case "UPDATE_PASSWORD" -> {
                String[] updatePassword = strCommand.split(" ");
                String tableName = "users";
                String[] values = {updatePassword[1], updatePassword[2], updatePassword[3]};
                String[] fieldNames = {"user_name", "user_email", "user_login"};
                String[] fieldsToRetrieve = {"user_id"};
                Object[] result = database.fetchData(tableName, values, fieldNames, fieldsToRetrieve);
                if (result.length > 0 && !"NULL".equals(result[0])) {
                    database.updateEmployeePassword((String) result[0], updatePassword[4], out);
                    out.writeObject("1");// Успех
                } else {
                    out.writeObject("0");// Пользователь не найден
                }
            }
            case "NAME" -> {

                // С клиента отправляется идентификатор пользователя, чтобы он мог работать в системе

                String[] clientName = strCommand.split(" ");
                // Сохранение потока вывода клиента
                synchronized (clientStreams) {
                    clientStreams.put(clientName[1], out);
                    startClientQueueProcessor(clientName[1], out);
                }

                String [] frandQuers = database.getRequestByUserId(clientName[1]);
                // Синхронизация данных для запросов в друзья, между клиентом и сервером

                if (frandQuers.length!=0){

                    for(String srt:frandQuers){

                        handleMessageTransfer(clientName[1], srt);

                    }

                }

                String [] frandOut = database.getResponseByUserId(clientName[1], "RESPONSE_SERVER");
                // Синхронизация данных для ответов на запросы в друзья, между клиентом и сервером


                if (frandOut.length!=0){

                    for(String srt:frandOut){

                        handleMessageTransfer(clientName[1], srt);

                    }

                }
                // Синхронизация данных для запросов на планирование бесед, между клиентом и сервером
                String [] messenfChat = database.getChatMesseng(clientName[1]);

                if (messenfChat.length!=0){

                    for(String srt:messenfChat){

                        handleMessageTransfer(clientName[1], srt);

                    }

                }

            }

            // Отправка уведомления об выходе пользователя из системы
            case "EXIT" -> {
                String[] clientNameExit = strCommand.split(" ");
                synchronized (clientStreams) {
                    clientStreams.remove(clientNameExit[1]);
                    clientQueues.remove(clientNameExit[1]);
                }
            }
            case "ONLINE" -> {
                // Перед отправкой сообщений клиент проверяет есть ли он в списке контактов получателей,
                // а так находится ли он в сети или нет
                String[] clientOnline = strCommand.split(" ");

                String online = clientStreams.containsKey(clientOnline[1]) ? "1" : "0";

                String contact = database.checkAccessRecord(clientOnline[1], clientOnline[2]);

                handleMessageTransfer(clientOnline[2], "ONLINE_PLUS_CONTACT " + contact + " " + online);
                // Только после этого возможна отправка сообщений
            }

            case "UNIQUENESS" -> {
                // Проверка уникальности логина, email и имени пользователя
                String[] update = strCommand.split(" ");
                String result = database.sendEmployeeDataUniqueness(update[1], "user_login") + " " +
                        database.sendEmployeeDataUniqueness(update[2], "user_email") + " " +
                        database.sendEmployeeDataUniqueness(update[3], "user_name");
                out.writeObject(result);
            }
            case "MAIL" -> {
                // Генерация и отправка публичного ключа RSA клиенту
                // Необходимо для отправки кода подтверждения на электронную почту
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048); // Инициализация с ключом длиной 2048 бит
                KeyPair keyPair = keyPairGenerator.generateKeyPair(); // Генерация пары ключей
                out.writeObject(keyPair.getPublic()); // Отправка публичного ключа
                threadLocalPrivateKey.set(keyPair.getPrivate()); // Сохранение приватного ключа в потоке
            }
            case "CODE" -> {
                // Расшифровка и отправка кода подтверждения по email
                String[] update = strCommand.split(" ");
                String decryptedString = safety.decryptWithRSA(threadLocalPrivateKey.get(), update[1]);
                new Email().sendConfirmationCode(decryptedString, update[2], PASSWORD);
            }
            case "KEY", "MESS_KEY" -> {
                // Синхронизация ключей и алгоритмов шифрования при отправки сообщений
                String[] update = strCommand.split(" ");
                handleMessageTransfer(update[2], strCommand);
            }
            case "NEW_CONTACT" -> {
                // Определяется можно ли отправлять запрос в друзья пользователю и не заблокировал ли он отправителя
                String[] update = strCommand.split(" ");
                boolean contactNew = database.isUserExists(update[2]);

                if(contactNew){

                    boolean locBlock = database.isLockFlagTrue(update[2], update[1]);

                    handleMessageTransfer(update[1], "NEW_CONTACT " + true + " " + locBlock);
                }else {

                    handleMessageTransfer(update[1], "NEW_CONTACT " + false + " " + false);

                }
            }
            case "FRAND_ADD" -> {
                // Запрос на добавление в друзья

                String[] getFreands = strCommand.split(" ");

                if(clientStreams.containsKey(getFreands[1])){

                    // Если получатель онлайн

                    database.insert_request(getFreands[1], getFreands[2], getFreands[3], false, getFreands[4]);
                    String [] frandQuers = database.getRequestByUserId(getFreands[1]);

                    if (frandQuers.length!=0){

                        for(String srt:frandQuers){

                            handleMessageTransfer(getFreands[1], srt);

                        }

                    }

                }else {
                    // Если получатель не онлайн
                    database.insert_request(getFreands[1], getFreands[2], getFreands[3], false, getFreands[4]);
                }

            }

            case "CHECK_UP" -> {
                // Проверка данных пользователя (имя, логин, email)
                String[] updatePassword = strCommand.split(" ");

                String result = database.sendEmployeeData("users",
                        new String[]{updatePassword[1], updatePassword[2], updatePassword[3]},
                        new String[]{"user_name", "user_login", "user_email"});

                out.writeObject(result);
            }

            case "NOT_BLOCK_KLIENT" -> {
                // Разблокировка пользователя, чтобы он имел возможность отправлять запросы в друзья

                String[] updatePassword = strCommand.split(" ");

                database.updateLockFlagByUserId(updatePassword[1], false, updatePassword[2]);

            }

            case "CONNECT_CONTACTS" -> {
                // Синхронизация идентификаторов контактов для пользователя, чтобы можно было
                // давать доступ на переписку другим пользователя

                String[] massang = strCommand.split(" ");

                database.insertAccessRecord(massang[2], massang[1]);

            }

            case "CONNECT_CONTACTS_PLUSS" -> {

                String[] massang = strCommand.split(" ");
                // Синхронизация идентификаторов контактов для пользователя, чтобы можно было
                // давать доступ на переписку другим пользователя
                database.insertAccessRecord(massang[2], massang[1]);
                // Синхронизация добавления контакта между сервером и клиентом
                database.insertAccessRecordPluss(massang[2], massang[1], massang[3], massang[4]);

            }

            case "CONNECT_CONTACTS_DELETE" -> {
                // Синхронизация удаления записи из списка идентификаторов контактов на сервере

                String[] massang = strCommand.split(" ");
                database.deleteAccessRecord(massang[2], massang[1]);

            }

            case "UP_CONTACT_NEW" -> {

                // Синхронизация обновления записи в списке контактов пользователя
                String[] massang = strCommand.split(" ");
                database.upContactNew(massang[1], massang[2], massang[3]);

            }

            case "CONTACT_CTAT_DB" -> {
                //Проверка перед отправкой запроса на начало беседы пользователю, проверяет есть ли идентификатор
                //отправителя в списке идентификаторов получателя
                String[] clientChat = strCommand.split(" ");

                String contact = database.checkAccessRecord(clientChat[2], clientChat[1]);

                handleMessageTransfer(clientChat[1], "CONTACT_CHAT_PLAN " + contact);
            }

            case "CONNECT_CONTACTS_DELETE_PLUSS" -> {

                String[] massang = strCommand.split(" ");
                // Синхронизация удаления записи из списка контактов на сервере

                database.deleteAccessRecordPluss(massang[2], massang[1]);

                // Синхронизация удаления записи из списка идентификаторов контактов на сервере

                database.deleteAccessRecord(massang[2], massang[1]);

            }

            case "CHAT_GET_1" -> {

                // Отправка заявки на планирование беседы, если отправляется только время начала беседы
                // Если получатель в сети, то заявка отправляется сразу, иначе она помещается во временное хранилище на сервере

                String tableName = "scheduling_requests";
                if (database.isTableExists(tableName)) {
                    System.out.println("Таблица " + tableName + " существует.");
                } else {
                    System.out.println("Таблица " + tableName + " не найдена.");
                }

                String[] updatePassword = strCommand.split(" ");

                if(clientStreams.containsKey(updatePassword[1])){

                    handleMessageTransfer(updatePassword[1], strCommand);

                }else {

                    String newRecordId = safety.generateClientId();

                    List<String> fields = List.of("request_id", "user_id", "sender_id", "start_time", "notification_status");
                    List<Object> values = List.of(newRecordId, updatePassword[1], updatePassword[2], updatePassword[3], false);

                    database.insertRecord("scheduling_requests", fields, values);

                }

            }

            case "CHAT_GET_1_2" -> {
                // Отправка заявки на планирование беседы, если отправляется время начала беседы и время окончания беседы
                // Если получатель в сети, то заявка отправляется сразу, иначе она помещается во временное хранилище на сервере

                String tableName = "scheduling_requests";
                if (database.isTableExists(tableName)) {
                    System.out.println("Таблица " + tableName + " существует.");
                } else {
                    System.out.println("Таблица " + tableName + " не найдена.");
                }

                String[] updatePassword = strCommand.split(" ");

                if(clientStreams.containsKey(updatePassword[1])){

                    handleMessageTransfer(updatePassword[1], strCommand);

                }else {

                    String newRecordId = safety.generateClientId();

                    List<String> fields = List.of("request_id", "user_id", "sender_id", "start_time", "end_time", "notification_status");
                    List<Object> values = List.of(newRecordId, updatePassword[1], updatePassword[2], updatePassword[3], updatePassword[4], false);

                    database.insertRecord("scheduling_requests", fields, values);

                }

            }

            case "CHAT_GET_1_3" -> {

                // Отправка заявки на планирование беседы, если отправляется время начала беседы и небольшое сообщение
                // Если получатель в сети, то заявка отправляется сразу, иначе она помещается во временное хранилище на сервере

                String tableName = "scheduling_requests";
                if (database.isTableExists(tableName)) {
                    System.out.println("Таблица " + tableName + " существует.");
                } else {
                    System.out.println("Таблица " + tableName + " не найдена.");
                }

                String[] updatePassword = strCommand.split(" ");

                if(clientStreams.containsKey(updatePassword[1])){

                    handleMessageTransfer(updatePassword[1], strCommand);

                }else {

                    String newRecordId = safety.generateClientId();

                    List<String> fields = List.of("request_id", "user_id", "sender_id", "messages", "start_time", "notification_status");
                    List<Object> values = List.of(newRecordId, updatePassword[1], updatePassword[2], updatePassword[4], updatePassword[3], false);

                    database.insertRecord("scheduling_requests", fields, values);

                }

            }

            case "CHAT_GET_1_2_3" -> {

                // Отправка заявки на планирование беседы, если отправляется время начала беседы, время окончания
                // беседы и небольшое сообщение
                // Если получатель в сети, то заявка отправляется сразу, иначе она помещается во временное хранилище на сервере

                String tableName = "scheduling_requests";
                if (database.isTableExists(tableName)) {
                    System.out.println("Таблица " + tableName + " существует.");
                } else {
                    System.out.println("Таблица " + tableName + " не найдена.");
                }

                String[] updatePassword = strCommand.split(" ");

                if(clientStreams.containsKey(updatePassword[1])){

                    handleMessageTransfer(updatePassword[1], strCommand);

                }else {

                    String newRecordId = safety.generateClientId();

                    List<String> fields = List.of("request_id", "user_id", "sender_id", "messages", "start_time",
                            "end_time", "notification_status");
                    List<Object> values = List.of(newRecordId, updatePassword[1], updatePassword[2], updatePassword[5], updatePassword[3],
                            updatePassword[4], false);

                    database.insertRecord("scheduling_requests", fields, values);

                }

            }

            case "ANSWER_FRIENDS_SERVER" -> {

                // Обработчик ответов на запросы в друзья, если пользователь в сети, то запрос отправляется ему сразу,
                // иначе помещается в хранилище на сервере

                String[] updatePassword = strCommand.split(" ");

                database.updateLockFlagByUserId(updatePassword[2], Boolean.parseBoolean(updatePassword[7]), updatePassword[1]);

                if(clientStreams.containsKey(updatePassword[2])){

                    database.insert_application_response(updatePassword[2], updatePassword[1],
                            updatePassword[3], updatePassword[4], Boolean.parseBoolean(updatePassword[7]), Boolean.parseBoolean(updatePassword[6]), false);
                    String [] frandQuers = database.getResponseByUserId(updatePassword[2], "RESPONSE");

                    if (frandQuers.length!=0){

                        for(String srt:frandQuers){

                            handleMessageTransfer(updatePassword[2], srt);

                        }

                    }

                }else {
                    database.insert_application_response(updatePassword[2], updatePassword[1],
                            updatePassword[3], updatePassword[4], Boolean.parseBoolean(updatePassword[7]),
                            Boolean.parseBoolean(updatePassword[6]), false);
                }

            }
        }
    }

    private static void processArrayCommand(String[] parts, ObjectOutputStream out) throws IOException {

        if(parts[0].equals("SYNCHRONY_OLL_CHAT")){

            // Синхронизация таблицы доступа для чата

            if(parts.length >= 2){

                database.syncAccessTable(parts[1], parts);

            }

        }else {

            if(parts[0].equals("SYNCHRONY_OLL_CHAT_PLUSS")){

                // Синхронизация контактов с сервером

                String result = database.syncContactsWithChatList(parts);

                if (result != null){
                    handleMessageTransfer(parts[1], result);
                }else {
                    handleMessageTransfer(parts[1], "SYNCHRONY_OLL_CHAT_PLUSS");
                }

            }else {

                if(parts[0].equals("SYNCHRONY_OLL_CHAT_PLUSS_DEL")){

                    // Удаление контактов

                    String[] newArray = Arrays.copyOfRange(parts, 1, parts.length);

                    database.deleteContactsByRecordIds(newArray);

                }else {

                    if(parts[0].equals("KEY_EXIT")){

                        // Обновление ключей чата, для возможности осуществления переписки

                        String[] newArray = Arrays.copyOfRange(parts, 2, parts.length);

                        try {
                            database.updateSessionKeyReserves(parts[1], newArray);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                    }else {

                        if(parts[0].equals("BLOCK_OLL")){

                            // Получение списка заблокированных пользователей

                            if (parts.length >= 2) { // Проверяем, что есть хотя бы 2 элемента

                                if (parts.length == 2) {
                                    String[] result = database.getFriendYouBlockArray(parts[1]);

                                    if (result.length != 0) {
                                        for (int i = 0; i < result.length; i++) {
                                            handleMessageTransfer(parts[1], result[i]);
                                            // Отправка списка
                                        }
                                    }
                                } else {
                                    String[] newArray = safety.removeFirstTwoElements(parts);
                                    // Удаление первых двух элементов
                                    String[] result = database.getFriendYouBlockArray(parts[1], newArray);
                                    // Получение по фильтру

                                    if (result.length != 0) {
                                        for (int i = 0; i < result.length; i++) {
                                            handleMessageTransfer(parts[1], result[i]);
                                            // Отправка списка
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Ошибка: массив parts слишком короткий, длина = " + parts.length);
                            }

                        }else {

                            if(!parts[0].equals("FRIEND_BLOCK")){

                                if (parts.length == 4) {

                                    // Переданные пользователей при регистрации

                                    database.inserting_data_into_table(parts);

                                } else {
                                    out.writeObject("Неверный формат данных");
                                }

                            }

                        }

                    }

                }

            }

        }

    }


    // Запускает обработчик очереди для клиента
    // - Создаёт приоритетную очередь для сообщений и файлов
    // - Запускает отдельный поток для асинхронной отправки данных клиенту
    // - Обрабатывает прерывания и ошибки ввода-вывода, очищая данные клиента при необходимости

    private static void startClientQueueProcessor(String clientName, ObjectOutputStream out) {
        PriorityBlockingQueue<QueueItem> queue = new PriorityBlockingQueue<>();
        synchronized (clientQueues) {
            if (!clientQueues.containsKey(clientName)) {
                clientQueues.put(clientName, queue);
                new Thread(() -> {
                    while (true) {
                        try {
                            QueueItem item = queue.take();
                            synchronized (out) { // Синхронизация на уровне записи
                                out.writeObject(item.getData());
                                out.flush();
                            }
                        } catch (InterruptedException e) {
                            cleanupClient(clientName); // Удаляем клиента при прерывании
                            break;
                        } catch (IOException e) {
                            cleanupClient(clientName); // Удаляем клиента при ошибке ввода-вывода
                            break;
                        }
                    }
                }).start();
            }
        }
    }


    // Удаляет клиента из хранилищ при завершении соединения или ошибке
    // Удаляет поток вывода и очередь клиента для освобождения ресурсов

    private static void cleanupClient(String clientName) {
        synchronized (clientStreams) {
            clientStreams.remove(clientName);
            clientQueues.remove(clientName);
        }
    }

    // Передаёт сообщение клиенту через очередь
    // - Проверяет, онлайн ли получатель
    // - Добавляет сообщение в очередь получателя с приоритетом 0
    // - Логирует, если клиент не найден

    private static void handleMessageTransfer(String recipientName, String message) {
        synchronized (clientStreams) {
            if (clientStreams.containsKey(recipientName)) {
                PriorityBlockingQueue<QueueItem> queue = clientQueues.get(recipientName);
                if (queue != null) {
                    queue.put(new QueueItem(message, 0)); // Приоритет 0 для сообщений
                }
            } else {
                System.out.println("Клиент с именем " + recipientName + " не найден.");
            }
        }
    }

    // Передаёт файл клиенту через очередь
    // - Проверяет, онлайн ли получатель
    // - Добавляет данные файла в очередь получателя с приоритетом 1

    private static void handleFileTransfer(FileTransferData data) {
        synchronized (clientStreams) {
            String recipientName = data.getRecipientName(); // Получение имени получателя
            if (clientStreams.containsKey(recipientName)) {
                PriorityBlockingQueue<QueueItem> queue = clientQueues.get(recipientName); // Получение очереди
                if (queue != null) {
                    queue.put(new QueueItem(data, 1)); // Добавление файла с приоритетом 1
                }
            }
        }
    }
}

