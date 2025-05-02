package org.face_recognition;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

// Класс Email используется для отправки писем с кодом подтверждения

public class Email {

    // Отправка кода подтверждения на указанную почту

    private static Safety safety = new Safety();
    public static void sendConfirmationCode(String to, String code, String passwordMy) throws Exception {
        Config config = new Config();
        String[] configMail = config.configEmail();  // Получаем настройки электронной почты

        final String from = safety.chaha20Decrypt(passwordMy,configMail[0]);     // Адрес отправителя
        final String password = safety.chaha20Decrypt(passwordMy,configMail[1]); // Пароль отправителя
        String host = safety.chaha20Decrypt(passwordMy,configMail[2]);           // SMTP-сервер для Mail.ru

        // Настройки для отправки письма
        Properties props = new Properties();
        props.put(safety.chaha20Decrypt(passwordMy,configMail[3]), safety.chaha20Decrypt(passwordMy,configMail[5]));    // auth: true/false
        props.put(safety.chaha20Decrypt(passwordMy,configMail[4]), safety.chaha20Decrypt(passwordMy,configMail[5]));    // SSL/TLS connection
        props.put(safety.chaha20Decrypt(passwordMy,configMail[6]), host);             // хост
        props.put(safety.chaha20Decrypt(passwordMy,configMail[7]), safety.chaha20Decrypt(passwordMy,configMail[8]));    // порт
        props.put(safety.chaha20Decrypt(passwordMy,configMail[9]), safety.chaha20Decrypt(passwordMy,configMail[10]));   // сообщение, позволяющее отправку без безопасного соединения

        // Создание сессии для отправки email
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password); // Аутентификация отправителя
            }
        });

        // Создание сообщения
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));            // Устанавливаем адрес отправителя
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to)); // Устанавливаем получателя
        message.setSubject("Код подтверждения регистрации");   // Устанавливаем тему письма
        message.setText("Ваш код подтверждения: " + code);    // Тело письма с кодом подтверждения

        // Отправка сообщения
        Transport.send(message); // Отправка письма
    }
}
