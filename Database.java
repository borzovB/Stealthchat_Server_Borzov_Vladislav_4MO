package org.face_recognition;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

// Класс Database предоставляет методы для работы с базой данных, такие как:
// - Инициализация базы данных и таблиц
// - Проверка уникальности данных сотрудников
// - Обновление паролей
// - Вставка данных сотрудников

public class Database {

    // Параметры подключения к базе данных
    private static String URL;
    private static String USER; // Имя пользователя базы данных
    private static String PASSWORD; // Пароль для подключения к базе данных
    private static String dbName; // Название базы данных
    private static Map<String, ObjectOutputStream> clientStreams; // Карта потоков вывода для клиентов
    private static Safety safety = new Safety(); // Объект класса безопасности
    private static Config config = new Config(); // Объект конфигурации
    private String[] date; // Конфигурационные данные
    private static String password;

    // Конструктор класса Database
    // Инициализирует параметры базы данных на основе конфигурационного файла

    public Database(Map<String, ObjectOutputStream> clientStreams, String password) throws Exception {
        this.clientStreams = clientStreams;
        this.password = password;
        // Загрузка конфигурационных данных базы данных
        date = config.configDateBase();
        URL = safety.chaha20Decrypt(password, date[0]);
        USER = safety.chaha20Decrypt(password, date[1]);
        PASSWORD = safety.chaha20Decrypt(password, date[2]);
        dbName = safety.chaha20Decrypt(password, date[3]);
    }

    //Проверяет уникальность данных сотрудника в базе данных
    static String sendEmployeeDataUniqueness(String inputData, String field_number_1) {
        String query = "SELECT user_id, user_login, user_email, user_password, user_name FROM users";
        Argon2 argon2 = Argon2Factory.create(); // Создаем объект Argon2 для работы с паролями

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            boolean found = false; // Флаг, чтобы определить, найден ли сотрудник
            String result = null;

            while (resultSet.next()) {
                String str = resultSet.getString(field_number_1);

                // Сравниваем хеш с использованием Argon2
                if (argon2.verify(str, inputData.toCharArray())) {
                    result = "false"; // Данные не уникальны
                    found = true;
                    break; // Прекращаем цикл при совпадении
                }
            }

            // Если сотрудник не найден, возвращаем "true"
            if (!found) {
                result = "true";
            }

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Erro";
        }
    }

    //Проверяет данные сотрудника (имя и пароль) в базе данных
    static String sendEmployeeData(String[] inputData, String field_number_1, String field_number_2, String field_number_3) {
        String query = "SELECT user_id, user_login, user_email, user_password, user_name FROM users";
        Argon2 argon2 = Argon2Factory.create(); // Объект Argon2 для хеширования и проверки

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            boolean found = false; // Флаг для определения, найден ли сотрудник
            String result = null;

            while (resultSet.next()) {
                String nameId = resultSet.getString("user_id");
                String name = resultSet.getString(field_number_1);
                String password = resultSet.getString(field_number_2);
                String login = resultSet.getString(field_number_3);

                // Проверяем имя и пароль с использованием Argon2
                if (argon2.verify(name, inputData[1].toCharArray()) &&
                        argon2.verify(password, inputData[2].toCharArray()) &&
                        argon2.verify(login, inputData[3].toCharArray())) {
                    if (clientStreams.containsKey(nameId)) {
                        result = "Erro"; // Если клиент уже подключен
                    } else {
                        result = nameId; // Возвращаем ID
                    }
                    found = true;
                    break; // Прекращаем поиск
                }
            }

            // Если сотрудник не найден, возвращаем "NULL"
            if (!found) {
                result = "NULL";
            }

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Erro";
        }
    }

    //Возвращает данные из таблицы в виде объекта, используется при обновлении пароля

    public static Object[] fetchData(String tableName, String[] plainValues, String[] fieldNames, String[] fieldsToRetrieve) throws SQLException {
        if (plainValues.length != fieldNames.length) {
            throw new IllegalArgumentException("Длина массива значений должна совпадать с длиной массива имен полей для фильтрации.");
        }

        // Формируем запрос для извлечения всех строк
        StringBuilder queryBuilder = new StringBuilder("SELECT ");
        queryBuilder.append(String.join(", ", fieldsToRetrieve));
        queryBuilder.append(", ").append(String.join(", ", fieldNames)); // Чтобы получить хэши для проверки
        queryBuilder.append(" FROM ").append(tableName);

        String query = queryBuilder.toString();

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Выполняем запрос
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    boolean allMatch = true;

                    // Проверяем соответствие всех значений
                    for (int i = 0; i < fieldNames.length; i++) {
                        String storedHash = resultSet.getString(fieldNames[i]);
                        if (!verifyArgon2(plainValues[i], storedHash)) {
                            allMatch = false;
                            break;
                        }
                    }

                    if (allMatch) {
                        // Если все значения совпали, возвращаем указанные поля
                        Object[] result = new Object[fieldsToRetrieve.length];
                        for (int i = 0; i < fieldsToRetrieve.length; i++) {
                            result[i] = resultSet.getObject(fieldsToRetrieve[i]);
                        }
                        return result;
                    }
                }
            }
        }

        // Если запись не найдена, возвращаем массив с "NULL"
        return new Object[] {"NULL"};
    }

    // Метод для проверки пароля с использованием Argon2
    private static boolean verifyArgon2(String plainValue, String hashedValue) {
        Argon2 argon2 = Argon2Factory.create();
        try {
            return argon2.verify(hashedValue, plainValue);
        } catch (Exception e) {
            System.err.println("Ошибка верификации Argon2: " + e.getMessage());
            return false;
        }
    }

    // Проверяет, соответствует ли набор входных данных записям в указанной таблице базы данных
    // Сравнивает каждое входное значение с соответствующими полями таблицы, используя хэширование Argon2 для проверки
    // Возвращает "PLUSS", если найдена запись, где все поля совпадают, "NULL_PLUSS", если совпадений нет, или "Erro" при ошибке

    static String sendEmployeeData(String tableName, String[] inputData, String[] fields) {
        // Проверка соответствия размеров массивов полей и входных данных
        if (fields.length != inputData.length) {
            throw new IllegalArgumentException("Количество полей и входных данных должно совпадать.");
        }

        // Формирование SQL-запроса для выборки всех записей из указанной таблицы
        String query = String.format("SELECT * FROM %s", tableName);
        // Создание объекта Argon2 для проверки хэшей
        Argon2 argon2 = Argon2Factory.create();

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Выполнение запроса и получение результата
            ResultSet resultSet = preparedStatement.executeQuery();

            // Перебор всех записей в результате
            while (resultSet.next()) {
                boolean allFieldsMatch = true; // Флаг совпадения всех полей

                // Проверка каждого поля на совпадение с входными данными
                for (int i = 0; i < fields.length; i++) {
                    // Получение значения поля из текущей записи
                    String fieldValue = resultSet.getString(fields[i]);
                    // Проверка, соответствует ли хэш поля входным данным
                    if (!argon2.verify(fieldValue, inputData[i].toCharArray())) {
                        allFieldsMatch = false; // Устанавливаем флаг, если поле не совпадает
                        break; // Прерываем цикл при первом несовпадении
                    }
                }

                // Если все поля совпали, возвращаем успех
                if (allFieldsMatch) {
                    return "PLUSS"; // Возврат результата при полном совпадении
                }
            }

            // Если совпадений не найдено, возвращаем "NULL_PLUSS"
            return "NULL_PLUSS";

        } catch (SQLException e) {
            // Вывод стека ошибки в случае SQL-исключения
            e.printStackTrace();
            // Возврат строки ошибки
            return "Erro";
        }
    }

    //Обновляет пароль сотрудника в базе данных
    public static void updateEmployeePassword(String nameID, String newPassword, ObjectOutputStream out) {
        String query = "UPDATE users SET user_password = ? WHERE user_id = ?";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, nameID);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                out.writeObject("1"); // Успешное обновление
            } else {
                out.writeObject("0"); // Обновление не выполнено
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    //Вставляет данные в таблицу заявок
    public static void insert_request(String user_id, String send_id, String public_key, boolean notifi, String getFreand) {
        String newRecordId = safety.generateClientId(); // Генерируем уникальный record_id

        String insertQuery = "INSERT INTO application_requests " +
                "(record_id, user_id, sender_id, notification_status, lock_flag, public_key, record_ac_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            // Установка значений для полей
            preparedStatement.setString(1, newRecordId);           // record_id (TEXT)
            preparedStatement.setString(2, user_id);             // user_id (TEXT)
            preparedStatement.setString(3, send_id);             // sender_id (TEXT)
            preparedStatement.setBoolean(4, notifi);               // notification_status (BOOLEAN, по умолчанию FALSE)
            preparedStatement.setBoolean(5, false);                // lock_flag (BOOLEAN, по умолчанию TRUE)
            preparedStatement.setString(6, public_key);             // public_key (TEXT)
            preparedStatement.setString(7, getFreand);             // public_key (TEXT)

            // Выполнение запроса
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Ошибка при вставке данных в application_requests:");
            e.printStackTrace();
        }
    }

    // Вставляет данные в таблицу application_responses, которая хранит ответы на заявки в друзья
    // Метод создаёт новую запись с уникальным идентификатором и сохраняет информацию о пользователе,
    // отправителе заявки, ключе шифрования, связанной записи друга, а также флаги блокировки,
    // статуса заявки и уведомления

    public static void insert_application_response(String user_id, String sender_id,
                                                   String key_encrypt, String record_ac_id_friend,
                                                   boolean lock_flag, boolean request_status, boolean notification_status) {

        // Генерация уникального идентификатора для новой записи
        String newRecordId = safety.generateClientId(); // Предполагается, что метод safety.generateClientId() существует

        // SQL-запрос для вставки данных в таблицу application_responses
        String insertQuery = "INSERT INTO application_responses " +
                "(record_id, user_id, sender_id, key_encrypt, " +
                "record_ac_id_friend, lock_flag, request_status, notification_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            // Установка параметров для SQL-запроса
            preparedStatement.setString(1, newRecordId);
            preparedStatement.setString(2, user_id);
            preparedStatement.setString(3, sender_id);
            preparedStatement.setString(4, key_encrypt);
            preparedStatement.setString(5, record_ac_id_friend);
            preparedStatement.setBoolean(6, lock_flag);
            preparedStatement.setBoolean(7, request_status);
            preparedStatement.setBoolean(8, notification_status);

            // Выполнение SQL-запроса для вставки данных
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            // Обработка ошибок SQL, например, проблемы с соединением или структурой таблицы
            System.err.println("Ошибка при вставке данных в application_responses:");
            e.printStackTrace(); // Вывод стека ошибки для диагностики
        }
    }

    public static void insertRecord(String tableName, List<String> fields, List<Object> values) {
        if (fields.size() != values.size()) {
            throw new IllegalArgumentException("Field and value lists must have the same size.");
        }

        StringBuilder query = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        query.append(String.join(", ", fields)).append(") VALUES (");
        query.append("?, ".repeat(fields.size()).replaceAll(", $", ")"));

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {

            int index = 1;
            for (Object value : values) {
                preparedStatement.setObject(index++, value);
            }

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при вставке данных в " + tableName + ":");
            e.printStackTrace();
        }
    }

    //  Получает заявки для указанного user_id из таблицы application_requests
    // Возвращает массив строк, каждая из которых представляет собой заявку в форматированном виде
    public static String[] getRequestByUserId(String user_id) {
        // SQL-запрос для получения данных о заявках, у которых notification_status = false
        String selectQuery = "SELECT record_id, sender_id, notification_status, lock_flag, public_key, record_ac_id " +
                "FROM application_requests WHERE user_id = ? AND notification_status = false";

        // Временный список для хранения строк с результатами запроса
        List<String> resultList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {

            // Установка значения для user_id в запрос
            preparedStatement.setString(1, user_id);

            // Выполнение запроса и получение результата
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    // Извлечение данных из результата запроса
                    String recordId = resultSet.getString("record_id"); // Уникальный идентификатор записи
                    String senderId = resultSet.getString("sender_id"); // ID отправителя
                    boolean notificationStatus = resultSet.getBoolean("notification_status"); // Статус уведомления
                    boolean lockFlag = resultSet.getBoolean("lock_flag"); // Флаг блокировки
                    String publicKey = resultSet.getString("public_key"); // Открытый ключ
                    String record_ac_id = resultSet.getString("record_ac_id"); // ID связанной записи

                    // Формирование строки с данными заявки
                    String record = "FRAND_ADD " + user_id + " " + senderId + " " + notificationStatus + " " + lockFlag + " " + publicKey + " " + record_ac_id + " " + recordId;
                    resultList.add(record); // Добавление строки в список

                    // Обновление notification_status на true после извлечения данных
                    String updateQuery = "UPDATE application_requests SET notification_status = true WHERE record_id = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, recordId);
                        updateStatement.executeUpdate(); // Выполнение запроса на обновление статуса
                    }
                }

            }

        } catch (SQLException e) {
            // Обработка ошибок при работе с базой данных
            System.err.println("Ошибка при извлечении данных из application_requests для user_id " + user_id + ":");
            e.printStackTrace();
        }

        // Преобразование списка в массив и возврат результата
        return resultList.toArray(new String[0]);
    }

    //Обработчик уведомлений на начало бесед, создает необходимые записи из данных таблицы scheduling_requests
    //для дальнейшей передачи их клиенту
    public static String[] getChatMesseng(String user_id) {
        String selectQuery = "SELECT request_id, sender_id, messages, start_time, end_time " +
                "FROM scheduling_requests WHERE user_id = ? AND notification_status = false";

        List<String> resultList = new ArrayList<>(); // Временный список для хранения строк

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {

            // Установка значения для user_id
            preparedStatement.setString(1, user_id);

            // Выполнение запроса и получение результата
            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    // Извлечение данных из результата
                    String recordId = resultSet.getString("request_id"); // Уникальный идентификатор записи
                    String senderId = resultSet.getString("sender_id");
                    String messages = resultSet.getString("messages");
                    String start_time = resultSet.getString("start_time");
                    String end_time = resultSet.getString("end_time");

                    String record = null;

                    if(end_time == null && messages == null){
                        record = "CHAT_GET_ONLINE_1 " + user_id + " " + senderId + " " + start_time;
                    }else {
                        if(end_time == null){
                            record = "CHAT_GET_ONLINE_1_3 " + user_id + " " + senderId + " " + messages + " " + start_time;
                        }else {
                            if(messages == null){
                                record = "CHAT_GET_ONLINE_1_2 " + user_id + " " + senderId + " " + start_time + " " + end_time;
                            }else {
                                record = "CHAT_GET_ONLINE_1_2_3 " + user_id + " " + senderId + " " + messages + " " + start_time + " " + end_time;
                            }
                        }
                    }

                    resultList.add(record);

                    // Обновление notification_status на true
                    String updateQuery = "UPDATE scheduling_requests SET notification_status = true WHERE request_id = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, recordId);
                        updateStatement.executeUpdate();
                    }
                }

                // Удаление записей с notification_status = true
                String deleteQuery = "DELETE FROM scheduling_requests WHERE notification_status = true";
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                    deleteStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при извлечении данных из application_requests для user_id " + user_id + ":");
            e.printStackTrace();
        }

        // Преобразование списка в массив и возврат
        return resultList.toArray(new String[0]);
    }

    //Возвращает массив строк, которые содержат ответы на заявки в друзья
    public static String[] getResponseByUserId(String user_id, String operation) {
        String selectQuery = "SELECT record_id, sender_id, key_encrypt, record_ac_id_friend, " +
                "lock_flag, request_status, notification_status " +
                "FROM application_responses WHERE user_id = ? AND notification_status = false";

        List<String> resultList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {

            preparedStatement.setString(1, user_id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<String> recordIdsToDelete = new ArrayList<>();

                while (resultSet.next()) {
                    String recordId = resultSet.getString("record_id");
                    String senderId = resultSet.getString("sender_id");
                    String keyEncrypt = resultSet.getString("key_encrypt");
                    String recordAcIdFriend = resultSet.getString("record_ac_id_friend");
                    boolean lockFlag = resultSet.getBoolean("lock_flag");
                    boolean requestStatus = resultSet.getBoolean("request_status");

                    String record = operation + " " + user_id + " " + senderId + " " + keyEncrypt + " " +
                            recordAcIdFriend + " " + lockFlag + " " + requestStatus;
                    resultList.add(record);

                    // Добавляем record_id в список для удаления после обработки
                    recordIdsToDelete.add(recordId);

                    // Обновляем notification_status на true
                    String updateQuery = "UPDATE application_responses SET notification_status = true WHERE record_id = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, recordId);
                        updateStatement.executeUpdate();
                    }
                }

                // Удаляем записи после обработки
                if (!recordIdsToDelete.isEmpty()) {
                    String deleteQuery = "DELETE FROM application_responses WHERE notification_status = ?";
                    try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                        deleteStatement.setBoolean(1, true);
                        deleteStatement.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при обработке данных из application_responses для user_id " + user_id + ":");
            e.printStackTrace();
        }

        return resultList.toArray(new String[0]);
    }


    //Проверяет, существует ли user_id в таблице users
    //Возвращает true, если найден user_id, иначе false
    public static boolean isUserExists(String userId) {
        String query = "SELECT 1 FROM users WHERE user_id = ? LIMIT 1";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next(); // Если есть хотя бы одна строка, возвращаем true
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // В случае ошибки считаем, что пользователя нет
        }
    }

    // Проверяет существование указанных таблиц в базе данных
    public static boolean areTablesExist(List<String> tableNames) {
        // Используем try-with-resources для управления соединением
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData(); // Получаем метаданные базы данных

            // Проверяем каждую таблицу
            for (String tableName : tableNames) {
                try (ResultSet resultSet = metaData.getTables(null, "public", tableName, new String[]{"TABLE"})) {
                    if (!resultSet.next()) {
                        return false; // Возвращаем false, если таблица не найдена
                    }
                }
            }
            return true; // Все таблицы найдены
        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            System.err.println("Ошибка при проверке таблиц:");
            e.printStackTrace();
            return false;
        }
    }

    // Вставляет запись в таблицу access, если она еще не существует
    public static void insertAccessRecord(String userId, String contactId) {
        // SQL-запрос для проверки существования записи
        String checkQuery = "SELECT 1 FROM access WHERE user_id = ? AND contact_id = ?";
        // SQL-запрос для вставки новой записи
        String insertQuery = "INSERT INTO access (record_id, user_id, contact_id) VALUES (?, ?, ?)";

        // Используем try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            // Проверяем, существует ли запись
            checkStmt.setString(1, userId);
            checkStmt.setString(2, contactId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) { // Если записи нет, вставляем новую
                String newRecordId = safety.generateClientId(); // Генерируем уникальный record_id
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, newRecordId);
                    insertStmt.setString(2, userId);
                    insertStmt.setString(3, contactId);
                    insertStmt.executeUpdate();
                }
            } else {
                // Логируем, если запись уже существует
                System.out.println("Запись уже существует для пользователя: " + userId + " и контакта: " + contactId);
            }
        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
        }
    }

    // Вставляет или обновляет запись в таблице contacts для указанного пользователя и контакта
    public static void insertAccessRecordPluss(String userId, String contactId, String name, String key) {
        // SQL-запрос для проверки существования записи
        String checkQuery = "SELECT record_id FROM contacts WHERE user_id = ? AND contact_id = ?";
        // SQL-запрос для вставки новой записи
        String insertQuery = "INSERT INTO contacts (record_id, user_id, contact_id, contact_name, session_key_reserve) " +
                "VALUES (?, ?, ?, ?, ?)";
        // SQL-запрос для обновления существующей записи
        String updateQuery = "UPDATE contacts SET contact_name = ?, session_key_reserve = ? " +
                "WHERE user_id = ? AND contact_id = ?";

        // Используем try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            // Проверяем, существует ли запись
            checkStmt.setString(1, userId);
            checkStmt.setString(2, contactId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) { // Если запись существует, обновляем
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, name);
                    updateStmt.setString(2, key);
                    updateStmt.setString(3, userId);
                    updateStmt.setString(4, contactId);
                    updateStmt.executeUpdate();
                }
            } else { // Если записи нет, вставляем новую
                String newRecordId = safety.generateClientId(); // Генерируем уникальный record_id
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, newRecordId);
                    insertStmt.setString(2, userId);
                    insertStmt.setString(3, contactId);
                    insertStmt.setString(4, name);
                    insertStmt.setString(5, key);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
        }
    }

    // Удаляет запись из таблицы access для указанного пользователя и контакта
    public static void deleteAccessRecord(String userId, String contactId) {
        // SQL-запрос для удаления записи
        String query = "DELETE FROM access WHERE user_id = ? AND contact_id = ?";
        // Используем try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, contactId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
        }
    }

    // Удаляет запись из таблицы contacts для указанного пользователя и контакта
    public static void deleteAccessRecordPluss(String userId, String contactId) {
        // SQL-запрос для удаления записи
        String query = "DELETE FROM contacts WHERE user_id = ? AND contact_id = ?";
        // Используем try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, contactId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
        }
    }

    // Удаляет записи из таблицы contacts по указанным идентификаторам записей
    public static void deleteContactsByRecordIds(String[] recordIds) {
        // Проверка на null или пустой массив
        if (recordIds == null || recordIds.length == 0) {
            System.out.println("Массив recordIds пустой, удаление не требуется.");
            return;
        }

        // SQL-запрос для удаления записи по record_id
        String sql = "DELETE FROM contacts WHERE record_id = ?";

        // Используем try-with-resources для управления соединением и запросом
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Добавление каждого record_id в батч для удаления
            for (String recordId : recordIds) {
                preparedStatement.setString(1, recordId);
                preparedStatement.addBatch(); // Добавляем в батч
            }

            // Выполнение батча
            preparedStatement.executeBatch();

        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
        }
    }

    // Инициализирует базу данных и таблицы, если они еще не существуют
    static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Проверяем наличие базы данных
            if (!doesDatabaseExist(conn, dbName)) {
                String createDBQuery = "CREATE DATABASE " + dbName;
                stmt.executeUpdate(createDBQuery);
            } else {
                System.out.println("База данных " + dbName + " уже существует.");
            }

            try (Connection newConn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
                 Statement newStmt = newConn.createStatement()) {

                // Создание таблицы users
                String createTableUsers = "CREATE TABLE IF NOT EXISTS users (" +
                        "user_id TEXT PRIMARY KEY, " +
                        "user_name TEXT NOT NULL, " +
                        "user_login TEXT UNIQUE NOT NULL, " +
                        "user_password TEXT NOT NULL, " +
                        "user_email TEXT UNIQUE NOT NULL" +
                        ");";
                newStmt.executeUpdate(createTableUsers);

                // Создание таблицы contacts
               String createTableContacts = "CREATE TABLE IF NOT EXISTS contacts (" +
                        "record_id TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL, " +
                        "contact_id TEXT NOT NULL, " +
                        "contact_name TEXT NOT NULL, " +
                        "session_key_reserve TEXT NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ");";
                newStmt.executeUpdate(createTableContacts);

                // Создание таблицы access
                String createTableAccess = "CREATE TABLE IF NOT EXISTS access (" +
                        "record_id TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL, " +
                        "contact_id TEXT NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ");";
                newStmt.executeUpdate(createTableAccess);

                // Создание таблицы scheduling_requests
                String createTableSchedulingRequests = "CREATE TABLE IF NOT EXISTS scheduling_requests (" +
                        "request_id TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL, " +
                        "sender_id TEXT NOT NULL, " +
                        "messages TEXT DEFAULT NULL, " +
                        "start_time TEXT NOT NULL, " +
                        "end_time TEXT DEFAULT NULL, " +
                        "notification_status BOOLEAN DEFAULT FALSE NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ");";
                newStmt.executeUpdate(createTableSchedulingRequests);

                // Создание таблицы application_responses
                String createTableApplicationResponses = "CREATE TABLE IF NOT EXISTS application_responses (" +
                        "record_id TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL, " +
                        "sender_id TEXT NOT NULL, " +
                        "key_encrypt TEXT NOT NULL, " +
                        "record_ac_id_friend TEXT NOT NULL, " +
                        "lock_flag BOOLEAN DEFAULT false NOT NULL, " +
                        "request_status BOOLEAN DEFAULT TRUE NOT NULL, " +
                        "notification_status BOOLEAN DEFAULT FALSE NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ");";
                newStmt.executeUpdate(createTableApplicationResponses);

                // Создание таблицы application_requests
                String createTableApplicationRequests = "CREATE TABLE IF NOT EXISTS application_requests (" +
                        "record_id TEXT PRIMARY KEY, " +
                        "user_id TEXT NOT NULL, " +
                        "sender_id TEXT NOT NULL, " +
                        "notification_status BOOLEAN DEFAULT FALSE NOT NULL, " +
                        "lock_flag BOOLEAN DEFAULT FALSE NOT NULL, " +
                        "public_key TEXT NOT NULL, " +
                        "record_ac_id TEXT NOT NULL, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ");";
                newStmt.executeUpdate(createTableApplicationRequests);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обновляет имя контакта в таблице contacts для указанного пользователя и контакта
    public void upContactNew(String myID, String senderId, String nameEnc) {
        // SQL-запрос для обновления contact_name
        String query = "UPDATE contacts SET contact_name = ? WHERE user_id = ? AND contact_id = ?";

        // Используем try-with-resources для управления соединением и запросом
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Установка параметров запроса
            statement.setString(1, nameEnc);  // Новое имя контакта
            statement.setString(2, myID);     // Идентификатор пользователя
            statement.setString(3, senderId); // Идентификатор контакта

            // Выполнение обновления
            statement.executeUpdate();

        } catch (SQLException e) {
            // Обработка и логирование ошибок SQL
            e.printStackTrace();
            System.err.println("Ошибка при обновлении имени контакта: " + e.getMessage());
        }
    }

    // Обновляет session_key_reserve для контактов пользователя в таблице contacts
    public void updateSessionKeyReserves(String userId, String[] contactDataArray) throws SQLException {
        // SQL-запрос для обновления session_key_reserve
        String updateQuery = "UPDATE contacts SET session_key_reserve = ? WHERE user_id = ? AND contact_id = ?";

        // Используем try-with-resources для управления соединением и запросом
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {

            // Обработка каждого элемента массива
            for (String contactData : contactDataArray) {
                // Разделяем строку на contact_id и session_key_reserve
                String[] parts = contactData.split(" ");
                if (parts.length != 2) {
                    continue; // Пропускаем некорректные строки
                }

                String contactId = parts[0];
                String sessionKeyReserve = parts[1];

                // Установка параметров запроса
                pstmt.setString(1, sessionKeyReserve);
                pstmt.setString(2, userId);
                pstmt.setString(3, contactId);

                // Выполнение обновления записи
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Вывод ошибки и проброс исключения
            e.printStackTrace();
            throw e;
        }
    }

    // Проверка наличия идентификатора контакта, в списке идентификатора контактов пользователя, необходимо для
    // осуществления переписки
    public String checkAccessRecord(String userId, String contactId) {
        String query = "SELECT 1 FROM access WHERE user_id = ? AND contact_id = ? LIMIT 1";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, userId);
            statement.setString(2, contactId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "1"; // Запись найдена
                } else {
                    return "0"; // Запись не найдена
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "0"; // В случае ошибки возвращаем "0" (можно изменить по вашим требованиям)
        }
    }

    // Синхронизирует таблицу access с переданным массивом контактов для указанного пользователя
    public void syncAccessTable(String userId, String[] contactsArray) {

        Connection conn = null;
        try {
            // Подключаемся к базе данных с вашими параметрами
            conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
            conn.setAutoCommit(false); // Начинаем транзакцию

            // 1. Получаем текущее состояние таблицы
            Set<String> currentRecords = new HashSet<>();
            String selectQuery = "SELECT contact_id, user_id FROM access WHERE user_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setString(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        String record = rs.getString("contact_id") + " " + rs.getString("user_id");
                        currentRecords.add(record);
                    }
                }
            }

            // 2. Преобразуем входной массив в Set, исключая первый элемент (индекс 0)
            Set<String> inputRecords = new HashSet<>();
            for (int i = 2; i < contactsArray.length; i++) { // Начинаем с i = 1
                inputRecords.add(contactsArray[i]);
            }

            // 3. Находим записи для удаления (есть в таблице, но нет в массиве)
            Set<String> recordsToDelete = new HashSet<>(currentRecords);
            recordsToDelete.removeAll(inputRecords);

            if (!recordsToDelete.isEmpty()) {
                String deleteQuery = "DELETE FROM access WHERE contact_id = ? AND user_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                    for (String record : recordsToDelete) {
                        String[] parts = record.split(" ");
                        deleteStmt.setString(1, parts[0]); // contact_id
                        deleteStmt.setString(2, parts[1]); // user_id
                        deleteStmt.addBatch();
                    }
                    deleteStmt.executeBatch();
                }
            }

            // 4. Находим записи для добавления (есть в массиве, но нет в таблице)
            Set<String> recordsToAdd = new HashSet<>(inputRecords);
            recordsToAdd.removeAll(currentRecords);

            if (!recordsToAdd.isEmpty()) {
                String insertQuery = "INSERT INTO access (record_id, contact_id, user_id) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    for (String record : recordsToAdd) {
                        String[] parts = record.split(" ");
                        String newClientId = safety.generateClientId(); // Генерируем уникальный record_id
                        insertStmt.setString(1, newClientId);
                        insertStmt.setString(2, parts[0]); // contact_id
                        insertStmt.setString(3, parts[1]); // user_id
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }
            }

            // 5. Фиксируем транзакцию
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // Откатываем изменения при ошибке
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    // Проверяет, существует ли запись для заданных user_id и sender_id,
    // где значение lock_flag равно true
    // Возвращает true, если такая запись найдена, иначе false

    public static boolean isLockFlagTrue(String userId, String senderId) {
        String query = "SELECT 1 FROM application_requests WHERE user_id = ? AND sender_id = ? AND lock_flag = true LIMIT 1";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, userId);
            statement.setString(2, senderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next(); // Если есть хотя бы одна строка, возвращаем true
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // В случае ошибки считаем, что условия не выполнены
        }
    }

    // Синхронизирует список контактов пользователя с переданным списком чатов
    // Обновляет существующие контакты, добавляет новые и возвращает отсутствующие в chatList контакты

    public String syncContactsWithChatList(String[] chatList) {
        // Проверка минимальной длины массива
        if (chatList.length < 2) {
            return null; // Возврат null, если массив пустой или слишком короткий
        }

        String userId = chatList[1]; // Извлечение user_id из второго элемента
        Set<String> chatContacts = new HashSet<>(); // Множество contact_id из chatList
        Map<String, String[]> chatDataMap = new HashMap<>(); // Мапа для хранения contact_name и session_key_reserve

        // Обработка элементов chatList, начиная с третьего, для создания мапы
        for (int i = 2; i < chatList.length; i++) {
            String[] parts = chatList[i].split(" ");
            if (parts.length >= 4) {
                String contactId = parts[1];
                chatContacts.add(contactId);
                chatDataMap.put(contactId, new String[]{parts[2], parts[3]}); // Сохранение contact_name и session_key_reserve
            }
        }

        List<String> missingContacts = new ArrayList<>(); // Список контактов, отсутствующих в chatList
        Set<String> duplicateContacts = new HashSet<>(); // Множество для отслеживания существующих contact_id

        // SQL-запросы для выборки, обновления и вставки данных
        String querySelect = "SELECT record_id, contact_id, contact_name, session_key_reserve FROM contacts WHERE user_id = ?";
        String queryUpdate = "UPDATE contacts SET contact_name = ?, session_key_reserve = ? WHERE user_id = ? AND contact_id = ?";
        String queryInsert = "INSERT INTO contacts (user_id, contact_id, contact_name, session_key_reserve, record_id) VALUES (?, ?, ?, ?, ?)";

        // Использование try-with-resources для управления соединением и запросами
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement selectStmt = connection.prepareStatement(querySelect);
             PreparedStatement updateStmt = connection.prepareStatement(queryUpdate);
             PreparedStatement insertStmt = connection.prepareStatement(queryInsert)) {

            selectStmt.setString(1, userId);
            // Выборка существующих контактов пользователя
            try (ResultSet resultSet = selectStmt.executeQuery()) {
                while (resultSet.next()) {
                    String record_id = resultSet.getString("record_id");
                    String contactId = resultSet.getString("contact_id");
                    String contactName = resultSet.getString("contact_name");
                    String sessionKeyReserve = resultSet.getString("session_key_reserve");

                    if (chatContacts.contains(contactId)) {
                        // Обновление данных контакта, если он есть в chatList
                        String[] updatedValues = chatDataMap.get(contactId);
                        updateStmt.setString(1, updatedValues[0]); // contact_name
                        updateStmt.setString(2, updatedValues[1]); // session_key_reserve
                        updateStmt.setString(3, userId);
                        updateStmt.setString(4, contactId);
                        updateStmt.executeUpdate();
                    } else {
                        // Добавление контакта в missingContacts, если его нет в chatList
                        missingContacts.add(record_id + " " + userId + " " + contactId + " " + contactName + " " + sessionKeyReserve);
                    }

                    duplicateContacts.add(contactId); // Отметка контакта как существующего
                }
            }

            // Вставка новых контактов из chatList, отсутствующих в базе
            for (String contactId : chatContacts) {
                if (!duplicateContacts.contains(contactId)) {
                    String[] contactData = chatDataMap.get(contactId);
                    insertStmt.setString(1, userId);
                    insertStmt.setString(2, contactId);
                    insertStmt.setString(3, contactData[0]); // contact_name
                    insertStmt.setString(4, contactData[1]); // session_key_reserve
                    insertStmt.setString(5, safety.generateClientId()); // Генерация уникального record_id
                    insertStmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            // Обработка ошибок SQL
            e.printStackTrace();
        }

        // Формирование результата с отсутствующими контактами
        if (!missingContacts.isEmpty()) {
            StringBuilder result = new StringBuilder("SYNCHRONY_OLL_CHAT_PLUSS ");
            result.append(String.join(" | ", missingContacts));
            return result.toString();
        }

        return null; // Возврат null, если нет отсутствующих контактов
    }

    // Метод проверяет существование таблиц
    public static boolean isTableExists(String tableName) {
        boolean exists = false;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                if (resultSet.next()) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке существования таблицы: " + tableName);
            e.printStackTrace();
        }

        return exists;
    }

    // Получает массив идентификаторов пользователей, заблокированных указанным пользователем,
    // исключая указанные sender_id из результата

    public static String[] getFriendYouBlockArray(String userId, String[] excludeSenderIds) {
        // SQL-запрос для выборки sender_id с lock_flag = TRUE для указанного user_id
        String selectQuery = "SELECT sender_id " +
                "FROM application_requests " +
                "WHERE user_id = ? AND lock_flag = TRUE " +
                "GROUP BY sender_id";
        List<String> resultList = new ArrayList<>(); // Список для хранения результатов

        // Преобразование массива excludeSenderIds в Set для эффективного поиска
        Set<String> excludeSet = new HashSet<>();
        if (excludeSenderIds != null) {
            excludeSet.addAll(Arrays.asList(excludeSenderIds));
        }

        // Использование try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD)) {
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                    pstmt.setString(1, userId); // Установка параметра user_id

                    // Выполнение запроса и обработка результатов
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String senderId = rs.getString("sender_id"); // Получение sender_id
                            // Добавление sender_id, если он не в списке исключений
                            if (!excludeSet.contains(senderId)) {
                                String formattedString = "YOU_BLOCK " + senderId; // Форматирование результата
                                resultList.add(formattedString); // Добавление в список
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Обработка ошибок SQL
            System.out.println("Ошибка при получении данных из application_requests: " + e.getMessage());
        }

        // Преобразование списка в массив строк
        return resultList.toArray(new String[0]);
    }

    // Получает массив идентификаторов пользователей, заблокированных указанным пользователем

    public static String[] getFriendYouBlockArray(String userId) {
        // SQL-запрос для выборки sender_id с lock_flag = TRUE для указанного user_id
        String selectQuery = "SELECT sender_id " +
                "FROM application_requests " +
                "WHERE user_id = ? AND lock_flag = TRUE " +
                "GROUP BY sender_id";
        List<String> resultList = new ArrayList<>(); // Список для хранения результатов

        // Использование try-with-resources для управления соединением и запросом
        try (Connection conn = DriverManager.getConnection(URL + dbName, USER, PASSWORD)) {
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                    pstmt.setString(1, userId); // Установка параметра user_id

                    // Выполнение запроса и обработка результатов
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String senderId = rs.getString("sender_id"); // Получение sender_id
                            String formattedString = "YOU_BLOCK " + senderId; // Форматирование результата
                            resultList.add(formattedString); // Добавление в список
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Обработка ошибок SQL
            System.out.println("Ошибка при получении данных из application_requests: " + e.getMessage());
        }

        // Преобразование списка в массив строк
        return resultList.toArray(new String[0]);
    }

    // Обновляет флаг блокировки (lock_flag) в таблице application_requests для записи,
    // соответствующей указанным sender_id и user_id

    public static void updateLockFlagByUserId(String sender_id, boolean newLockFlag, String ac_id) {

        // SQL-запрос для обновления lock_flag в таблице application_requests
        String updateQuery = "UPDATE application_requests SET lock_flag = ? WHERE sender_id = ? AND user_id = ?";

        // Использование try-with-resources для автоматического закрытия соединения и запроса
        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            // Установка параметров запроса
            preparedStatement.setBoolean(1, newLockFlag);
            preparedStatement.setString(2, sender_id);
            preparedStatement.setString(3, ac_id);

            // Выполнение запроса
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            // Обработка ошибок SQL
            System.err.println("Ошибка при обновлении lock_flag для sender_id " + sender_id + ":");
            e.printStackTrace();
        }
    }

    // Вставляет данные сотрудника в таблицу
    public static void inserting_data_into_table(String[] parts) {
        String newClientId = safety.generateClientId();

        String insertQuery = "INSERT INTO users (user_id, user_name, user_email, user_password, user_login) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL + dbName, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {


            preparedStatement.setString(1, newClientId);
            preparedStatement.setString(2, parts[0]);
            preparedStatement.setString(3, parts[1]);
            preparedStatement.setString(4, parts[2]);
            preparedStatement.setString(5, parts[3]);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Проверяет существование базы данных
    private static boolean doesDatabaseExist(Connection conn, String dbName) {
        String query = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dbName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
