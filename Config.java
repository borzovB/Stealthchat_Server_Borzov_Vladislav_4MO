package org.face_recognition;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//Класс для загрузки конфигурационных параметров из файла конфигурации `config.properties

public class Config {


    //Загрузка настроек базы данных
    public static String[] configDateBase() {
        Properties properties = new Properties();
        String[] configServer = new String[5]; // Массив для 5 параметров

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            // Загружаем данные из файла
            properties.load(fis);

            // Читаем параметры базы данных и заполняем массив
            configServer[0] = properties.getProperty("db.url_data_base");        // URL базы данных
            configServer[1] = properties.getProperty("db.user_date_base");        // Имя пользователя
            configServer[2] = properties.getProperty("db.password_date_base");    // Пароль
            configServer[3] = properties.getProperty("db.db_name");                // Название базы данных
            configServer[4] = properties.getProperty("db.table_name");             // Название таблицы

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке файла конфигурации: " + e.getMessage());
        }

        return configServer;
    }

    //Загрузка настроек сервера
    public static String[] configServer() {
        Properties properties = new Properties();
        String[] configServer = new String[5]; // Массив для 5 параметров

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            // Загружаем данные из файла
            properties.load(fis);

            // Читаем параметры сервера и заполняем массив
            configServer[0] = properties.getProperty("db.port_server");           // Порт сервера
            configServer[1] = properties.getProperty("db.name_key");               // Имя ключа
            configServer[2] = properties.getProperty("db.server");                 // Адрес сервера
            configServer[3] = properties.getProperty("db.name_ssl");               // Имя SSL
            configServer[4] = properties.getProperty("db.password_server");        // Пароль сервера

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке файла конфигурации: " + e.getMessage());
        }

        return configServer;
    }

    //Загрузка настроек электронной почты
    public static String[] configEmail() {
        Properties properties = new Properties();
        String[] configMail = new String[11]; // Массив для 11 параметров

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            // Загружаем данные из файла
            properties.load(fis);

            // Читаем параметры электронной почты и заполняем массив
            configMail[0] = properties.getProperty("db.email");           // Адрес электронной почты
            configMail[1] = properties.getProperty("db.password");        // Пароль электронной почты
            configMail[2] = properties.getProperty("db.host_mail");       // Хост для почты (например, Mail.ru)
            configMail[3] = properties.getProperty("db.mail_smtp");       // SMTP сервер
            configMail[4] = properties.getProperty("db.smtp");            // Протокол SMTP
            configMail[5] = properties.getProperty("db.bool");            // Признак включения/выключения (true/false)
            configMail[6] = properties.getProperty("db.host");            // Хост SMTP
            configMail[7] = properties.getProperty("db.mail_port");       // Порт почты
            configMail[8] = properties.getProperty("db.port_smtp");       // Порт SMTP
            configMail[9] = properties.getProperty("db.mail");            // Вторая почта
            configMail[10] = properties.getProperty("db.simv");           // Прочие символы или метки

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке файла конфигурации: " + e.getMessage());
        }

        return configMail;
    }

    // Создает файл config.properties, если он отсутствует
    public static void createConfigFile(String CONFIG_FILE_NAME) throws IOException {
        Path path = Paths.get(CONFIG_FILE_NAME);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    // Заполняет файл config.properties указанной парой ключ-значение
    public static void setConfigProperty(String key, String value, String CONFIG_FILE_NAME) throws IOException {
        Properties properties = new Properties();
        File file = new File(CONFIG_FILE_NAME);

        // Загружаем существующие свойства, если файл уже содержит данные
        if (file.exists() && file.length() > 0) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            }
        }

        // Добавляем новое свойство или обновляем значение существующего
        properties.setProperty(key, value);

        // Сохраняем изменения обратно в файл
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Updated configuration file");
        }
    }

    // Метод для получения значения свойства из config.properties
    public static String getProperty(String key, String CONFIG_FILE_NAME) throws IOException {
        Properties properties = new Properties();

        // Открываем файл и загружаем свойства
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            properties.load(fis);
        }

        // Возвращаем значение по ключу
        return properties.getProperty(key);
    }

}
