package org.face_recognition;

import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;

// Класс Safety предоставляет функции для обеспечения безопасности,
// такие как генерация уникального идентификатора клиента и расшифровка данных с использованием RSA

public class Safety {

    // Генерирует уникальный идентификатор клиента
    // Уникальный идентификатор основан на алгоритме UUID (Universally Unique Identifier),
    // который обеспечивает практически полную гарантию уникальности
    public static String generateClientId() {
        // Создаем уникальный идентификатор с использованием встроенного класса UUID
        UUID clientId = UUID.randomUUID();
        return clientId.toString(); // Преобразуем идентификатор в строку и возвращаем
    }

    // Расшифровывает данные с использованием RSA-шифрования и приватного ключа
    // Ожидается, что входные данные переданы в формате Base64

    public static String decryptWithRSA(PrivateKey privateKey, String encryptedData) throws Exception {
        // Инициализируем шифрование с использованием алгоритма RSA
        Cipher cipher = Cipher.getInstance("RSA");

        // Устанавливаем режим расшифровки и передаем приватный ключ
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Декодируем зашифрованные данные из формата Base64 в массив байтов
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);

        // Выполняем расшифровку данных
        byte[] decryptedData = cipher.doFinal(decodedData);

        // Преобразуем расшифрованные байты в строку и возвращаем
        return new String(decryptedData);
    }

    // Проверяет валидность пароля по заданным критериям
    // Требования к паролю:
    // - Минимум 8 символов
    // - Наличие хотя бы одной строчной буквы
    // - Наличие хотя бы одной заглавной буквы
    // - Наличие хотя бы одной цифры

    boolean isValidPassword(String password) {
        // Регулярное выражение для валидации
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }

    //Массив, который содержит параметры, необходимые для извлечения данных из файла конфигурации
    String [] configMass(){
        String conf_str [] = {
                "db.email", "db.password", "db.host_mail", "db.mail_smtp", "db.smtp", "db.bool", "db.host",
                "db.mail_port", "db.port_smtp", "db.mail", "db.simv", "db.port_server", "db.name_key",
                "db.server", "db.name_ssl", "db.password_server", "db.url_data_base", "db.user_date_base",
                "db.password_date_base", "db.db_name", "db.table_name"
        };
        return  conf_str;
    }

    // Удаляет первые два элемента из массива строк
    // Используется для обработки команд, где первые два элемента не нужны

    public static String[] removeFirstTwoElements(String[] parts) {
        if (parts == null || parts.length <= 2) {
            return new String[0];
        }

        ArrayList<String> list = new ArrayList<>(Arrays.asList(parts));
        list.subList(0, 2).clear(); // Удаляем первые два элемента

        return list.toArray(new String[0]);
    }

    // Шифрование
    String chaha20Encript(String password, String originalText) throws Exception {
        byte[] salt = generateSalt();

        // Генерация ключа из пароля и соли
        byte[] key = generateKeyFromPasswordChacha20(password, salt);

        // Генерация 12-байтового nonce из пароля
        byte[] nonce = generateNonce(password);

        // Шифрование
        String encryptedText = encryptChacha20(originalText, key, nonce);

        // Храним соль, nonce и зашифрованное сообщение вместе
        String storedData = Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(nonce) + ":" + encryptedText;

        return storedData;
    }

    // Генерация соли и хеширование пароля с солью
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];  // Соль размером 16 байт
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    // Генерация ключа из пароля и соли
    public static byte[] generateKeyFromPasswordChacha20(String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);  // Добавляем соль
        return digest.digest(password.getBytes());
    }

    // Генерация 12-байтового nonce (используем первые 12 байтов из хеширования пароля)
    public static byte[] generateNonce(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] passwordHash = digest.digest(password.getBytes());

        // Возвращаем только первые 12 байтов для nonce
        byte[] nonce = new byte[12];
        System.arraycopy(passwordHash, 0, nonce, 0, 12);
        return nonce;
    }

    //Метод шифрует текст с использованием алгоритма ChaCha20
    public static String encryptChacha20(String plainText, byte[] key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20");
        SecretKeySpec keySpec = new SecretKeySpec(key, "ChaCha20");
        ChaCha20ParameterSpec paramSpec = new ChaCha20ParameterSpec(nonce, 0);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Метод дешифрует текст с использованием алгоритма ChaCha20
    public static String decryptChaha20(String cipherText, byte[] key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20");
        SecretKeySpec keySpec = new SecretKeySpec(key, "ChaCha20");
        ChaCha20ParameterSpec paramSpec = new ChaCha20ParameterSpec(nonce, 0);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    //Дешифрование
    String chaha20Decrypt(String password, String storedData) throws Exception {
        // Извлекаем данные
        String[] parts = storedData.split(":");
        byte[] storedSalt = Base64.getDecoder().decode(parts[0]);
        byte[] storedNonce = Base64.getDecoder().decode(parts[1]);
        String storedCipherText = parts[2];

        // Генерация ключа с использованием соли
        byte[] storedKey = generateKeyFromPasswordChacha20(password, storedSalt);

        // Дешифрование
        String decryptedText = decryptChaha20(storedCipherText, storedKey, storedNonce);

        return  decryptedText;
    }

}
