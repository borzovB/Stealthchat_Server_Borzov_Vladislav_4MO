package org.face_recognition;

// Криптография, содержит обработку алгоритма хэширования

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class Encryption {
    public static String Argon2(String text){

        // Создаем экземпляр Argon2
        Argon2 argon2 = Argon2Factory.create();

        try {
            // Параметры Argon2: количество итераций, память (в KB), параллелизм
            int iterations = 3;
            int memory = 65536;
            int parallelism = 100;

            // Хешируем пароль
            String hash = argon2.hash(iterations, memory, parallelism, text.toCharArray());

            return hash;

        } finally {
            // Освобождаем ресурсы Argon2
            argon2.wipeArray(text.toCharArray());
        }
    }

}
