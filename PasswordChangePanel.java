package org.face_recognition;

// Класс PasswordChangePanel предоставляет графический интерфейс для изменения пароля в серверном приложении
// Основные функции:
// - Создание окна с полями для ввода нового пароля и его подтверждения
// - Валидация пароля с использованием класса Safety
// - Шифрование и обновление конфигурационных параметров в файлах config_user.properties и config.properties
// - Поддержка показа/скрытия пароля с автоматическим таймером для скрытия
// - Возврат в главное меню или запуск сервера после успешного изменения пароля

import javax.swing.*;
import java.awt.*;

public class PasswordChangePanel {

    private static String password; // Текущий пароль, используемый для расшифровки конфигурации
    private static final String CONFIG_FILE_NAME = "config_user.properties"; // Имя файла конфигурации пользователя
    private static final String CONFIG_FILE_NAME_SERVER = "config.properties"; // Имя основного файла конфигурации сервера
    private static Config config = new Config(); // Объект для работы с файлами конфигурации
    private static JFrame frame; // Главное окно приложения
    private static Encryption encryption = new Encryption(); // Объект для выполнения операций шифрования (Argon2)
    private static JPanel textPanel; // Панель для первого поля пароля и кнопки "Показать"
    private static JPanel textPanel1; // Панель для второго поля пароля и кнопки "Показать"
    private static StartServer startServer = new StartServer(); // Объект для запуска сервера
    private static Safety safety = new Safety(); // Объект для валидации паролей и шифрования (ChaCha20)

    // Конструктор PasswordChangePanel
    // Инициализирует текущий пароль, который будет использоваться для расшифровки конфигурации
    public PasswordChangePanel(String password) {
        this.password = password;
    }

    // Запускает графический интерфейс для изменения пароля
    // Создаёт окно с полями ввода пароля, кнопками для показа/скрытия пароля,
    // а также кнопками "Изменить" и "Вернуться в главное меню"

    public static void startChangePassword() {
        frame = new JFrame("Управление Сервером"); // Создание главного окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закрытие приложения при закрытии окна
        frame.setSize(600, 500); // Установка размера окна
        frame.setLocationRelativeTo(null); // Центрирование окна на экране

        Dimension minSize = new Dimension(640, 620); // Минимальный размер окна
        frame.setMinimumSize(minSize);

        JPanel mainPanel = new JPanel(); // Основная панель для размещения всех компонентов
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); // Вертикальное расположение компонентов
        mainPanel.setBackground(new Color(30, 30, 30)); // Тёмный фон панели
        mainPanel.setOpaque(true);

        mainPanel.add(Box.createVerticalGlue()); // Пространство сверху для центрирования

        JPanel panel = new JPanel(); // Панель для элементов интерфейса
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Вертикальное расположение
        panel.setBackground(new Color(60, 63, 65)); // Серый фон панели
        panel.setPreferredSize(new Dimension(500, 450)); // Предпочтительный размер
        panel.setMinimumSize(new Dimension(500, 450)); // Минимальный размер
        panel.setMaximumSize(new Dimension(500, 450)); // Максимальный размер
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Отступы по краям

        panel.add(Box.createRigidArea(new Dimension(0, 30))); // Пространство сверху

        JLabel label = new JLabel("Осуществление входа"); // Заголовок окна
        label.setAlignmentX(Component.CENTER_ALIGNMENT); // Центрирование
        label.setForeground(Color.WHITE); // Белый цвет текста
        label.setFont(new Font("Arial", Font.BOLD, 31)); // Шрифт заголовка
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 30))); // Пространство после заголовка

        // Первое поле для ввода нового пароля
        JPasswordField passwordField1 = new JPasswordField(); // Поле для ввода пароля
        passwordField1.setPreferredSize(new Dimension(250, 40)); // Размер поля
        passwordField1.setFont(new Font("Arial", Font.PLAIN, 16)); // Шрифт поля

        JButton showPasswordButton1 = new JButton(); // Кнопка для показа/скрытия пароля
        ControlPanel controlPanel = new ControlPanel(); // Объект для настройки поля (неясная роль)
        controlPanel.panalGO(passwordField1); // Настройка поля (предположительно добавление слушателей)
        textTime(passwordField1, showPasswordButton1); // Настройка кнопки показа/скрытия

        textPanel = new JPanel(); // Панель для первого поля и кнопки
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS)); // Горизонтальное расположение
        textPanel.add(showPasswordButton1); // Добавление кнопки
        textPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ
        textPanel.add(passwordField1); // Добавление поля
        textPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ

        // Второе поле для подтверждения пароля
        JPasswordField passwordField2 = new JPasswordField(); // Поле для повторного ввода
        passwordField2.setPreferredSize(new Dimension(250, 40)); // Размер поля
        passwordField2.setFont(new Font("Arial", Font.PLAIN, 16)); // Шрифт поля

        JButton showPasswordButton2 = new JButton(); // Кнопка для показа/скрытия второго пароля
        ControlPanel controlPanel2 = new ControlPanel(); // Объект для настройки второго поля
        controlPanel2.panalGO(passwordField2); // Настройка поля
        textTime(passwordField2, showPasswordButton2); // Настройка кнопки показа/скрытия

        // Панель для метки первого поля
        JPanel label1Panel = new JPanel(); // Панель для метки "Введите новый пароль"
        label1Panel.setLayout(new BorderLayout()); // Макет для выравнивания
        label1Panel.setBackground(new Color(60, 63, 65)); // Фон совпадает с панелью
        label1Panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20)); // Отступы

        JLabel label1 = new JLabel("Введите новый пароль"); // Метка первого поля
        label1.setForeground(Color.WHITE); // Белый цвет текста
        label1.setFont(new Font("Arial", Font.PLAIN, 17)); // Шрифт метки
        label1.setAlignmentX(Component.RIGHT_ALIGNMENT); // Выравнивание

        label1Panel.add(label1); // Добавление метки

        // Панель для метки второго поля
        JPanel label2Panel = new JPanel(); // Панель для метки "Повторите новый пароль"
        label2Panel.setLayout(new BorderLayout()); // Макет для выравнивания
        label2Panel.setBackground(new Color(60, 63, 65)); // Фон совпадает с панелью
        label2Panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20)); // Отступы

        JLabel label2 = new JLabel("Повторите новый пароль"); // Метка второго поля
        label2.setForeground(Color.WHITE); // Белый цвет текста
        label2.setFont(new Font("Arial", Font.PLAIN, 17)); // Шрифт метки
        label2.setAlignmentX(Component.RIGHT_ALIGNMENT); // Выравнивание

        label2Panel.add(label2); // Добавление метки

        textPanel1 = new JPanel(); // Панель для второго поля и кнопки
        textPanel1.setLayout(new BoxLayout(textPanel1, BoxLayout.X_AXIS)); // Горизонтальное расположение
        textPanel1.add(showPasswordButton2); // Добавление кнопки
        textPanel1.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ
        textPanel1.add(passwordField2); // Добавление поля
        textPanel1.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ

        panel.add(label1Panel); // Добавление метки первого поля
        panel.add(textPanel); // Добавление панели первого поля
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Пространство
        panel.add(label2Panel); // Добавление метки второго поля
        panel.add(textPanel1); // Добавление панели второго поля
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Пространство

        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Дополнительное пространство

        // Кнопка "Изменить"
        JButton entrance = new JButton("Изменить"); // Кнопка для подтверждения изменения
        customizeButton(entrance); // Настройка стиля кнопки
        entrance.addActionListener(e -> {
            char[] password1 = passwordField1.getPassword(); // Получение первого пароля
            char[] password2 = passwordField2.getPassword(); // Получение второго пароля

            // Запуск обработки в отдельном потоке для избежания блокировки UI
            new Thread(() -> {
                try {
                    handleEntrance(entrance, new String(password1), new String(password2)); // Обработка ввода
                } catch (Exception ex) {
                    throw new RuntimeException(ex); // Обработка исключений
                }
            }).start();
        });

        // Кнопка "Вернуться в главное меню"
        JButton showStartButton = new JButton("Вернуться в главное меню"); // Кнопка для возврата
        customizeButton(showStartButton); // Настройка стиля кнопки
        showStartButton.addActionListener(e -> {
            StartServer server = new StartServer(); // Создание объекта сервера
            frame.dispose(); // Закрытие текущего окна
            server.start(); // Запуск главного меню
        });

        // Добавление кнопок на панель
        panel.add(entrance);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Пространство между кнопками
        panel.add(showStartButton);

        panel.add(Box.createRigidArea(new Dimension(0, 80))); // Пространство снизу
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 10)); // Светло-серая рамка панели

        mainPanel.add(panel); // Добавление панели в основную панель
        mainPanel.add(Box.createVerticalGlue()); // Пространство снизу для центрирования

        frame.setContentPane(mainPanel); // Установка основной панели как содержимого окна
        frame.setVisible(true); // Отображение окна
    }

    // Настраивает кнопку показа/скрытия пароля для поля ввода
    // - Устанавливает иконки для состояний "показать" и "скрыть"
    // - Реализует переключение видимости пароля с таймером (10 секунд) для автоматического скрытия

    private static void textTime(JPasswordField passwordField, JButton showPasswordButton) {
        ImageIcon originalIconNot = new ImageIcon("eye_icon.png"); // Иконка для состояния "показать"
        ImageIcon originalIcon = new ImageIcon("eye_icon_not.png"); // Иконка для состояния "скрыть"

        // Масштабирование иконок до размера 40x40
        Image scaledImg = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImg); // Масштабированная иконка "скрыть"
        Image scaledImgNot = originalIconNot.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIconNot = new ImageIcon(scaledImgNot); // Масштабированная иконка "показать"

        showPasswordButton.setIcon(scaledIconNot); // Установка начальной иконки (пароль скрыт)
        showPasswordButton.setContentAreaFilled(false); // Удаление фона кнопки
        showPasswordButton.setBorder(BorderFactory.createEmptyBorder()); // Удаление рамки

        // Установка квадратного размера кнопки
        showPasswordButton.setPreferredSize(new Dimension(40, 40));
        showPasswordButton.setMaximumSize(new Dimension(40, 40));
        showPasswordButton.setMinimumSize(new Dimension(40, 40));

        final boolean[] isPasswordVisible = {false}; // Флаг состояния видимости пароля

        // Таймер для автоматического скрытия пароля через 10 секунд
        Timer hidePasswordTimer = new Timer(10000, event -> {
            passwordField.setEchoChar('•'); // Скрытие пароля
            isPasswordVisible[0] = false; // Обновление состояния
            showPasswordButton.setIcon(scaledIconNot); // Установка иконки "скрыть"
        });

        // Обработчик нажатия на кнопку показа/скрытия
        showPasswordButton.addActionListener(e -> {
            if (isPasswordVisible[0]) {
                passwordField.setEchoChar('•'); // Скрытие пароля
                isPasswordVisible[0] = false; // Обновление состояния
                hidePasswordTimer.stop(); // Остановка таймера
                showPasswordButton.setIcon(scaledIconNot); // Установка иконки "скрыть"
            } else {
                passwordField.setEchoChar((char) 0); // Показ пароля
                isPasswordVisible[0] = true; // Обновление состояния
                hidePasswordTimer.restart(); // Перезапуск таймера
                showPasswordButton.setIcon(scaledIcon); // Установка иконки "показать"
            }
        });
    }

    // Настраивает стиль кнопки (цвет, шрифт, отступы, курсор)
    // Применяется к кнопкам "Изменить" и "Вернуться в главное меню"

    private static void customizeButton(JButton button) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Центрирование кнопки
        button.setBackground(new Color(75, 110, 175)); // Синий фон
        button.setForeground(Color.WHITE); // Белый текст
        button.setFont(new Font("Arial", Font.BOLD, 16)); // Шрифт кнопки
        button.setFocusPainted(false); // Удаление обводки при фокусе
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Отступы внутри кнопки
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Курсор "рука"
        button.setPreferredSize(new Dimension(300, 80)); // Предпочтительный размер
        button.setMinimumSize(new Dimension(200, 60)); // Минимальный размер
        button.setMaximumSize(new Dimension(300, 60)); // Максимальный размер
    }

    // Обрабатывает нажатие кнопки "Изменить"
    // - Проверяет, заполнены ли оба поля пароля
    // - Валидирует новый пароль с использованием Safety.isValidPassword()
    // - Проверяет совпадение паролей
    // - Обновляет конфигурацию, шифруя новый пароль (Argon2) и перешифровывая параметры конфигурации (ChaCha20)
    // - Показывает сообщения об успехе или ошибке

    private static void handleEntrance(JButton entranceButton, String paswordNew1, String paswordNew2) throws Exception {
        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(false); // Отключение кнопки
            entranceButton.setText("Изменение..."); // Изменение текста кнопки
        });

        // Проверка на пустые поля
        if (paswordNew2.isEmpty() || paswordNew1.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Все поля должны быть заполнены.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                entranceButton.setEnabled(true); // Включение кнопки
                entranceButton.setText("Изменить"); // Восстановление текста
            });
            return;
        }

        // Проверка валидности пароля
        if (!safety.isValidPassword(paswordNew2)) {
            JOptionPane.showMessageDialog(frame, "Пароль должен содержать не менее 8 символов, " +
                    "включая строчные и прописные буквы и цифры.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                entranceButton.setEnabled(true); // Включение кнопки
                entranceButton.setText("Изменить"); // Восстановление текста
            });
            return;
        }

        // Проверка совпадения паролей
        if (paswordNew1.equals(paswordNew2)) {
            // Обновление пароля в конфигурации пользователя (хэширование с Argon2)
            config.setConfigProperty("db.password", encryption.Argon2(paswordNew2), CONFIG_FILE_NAME);
            String conf_str[] = safety.configMass(); // Получение списка параметров конфигурации

            // Перешифровка всех параметров конфигурации с новым паролем
            int i = 0;
            while (i < conf_str.length) {
                String confOld = config.getProperty(conf_str[i], CONFIG_FILE_NAME_SERVER); // Получение старого значения
                String newConf = safety.chaha20Decrypt(password, confOld); // Расшифровка старого значения
                String srt = safety.chaha20Encript(paswordNew2, newConf); // Шифрование с новым паролем
                config.setConfigProperty(conf_str[i], srt, CONFIG_FILE_NAME_SERVER); // Обновление параметра
                i++;
            }

            JOptionPane.showMessageDialog(frame, "Действие выполнено.", "Результат", JOptionPane.INFORMATION_MESSAGE);
            frame.dispose(); // Закрытие окна
            startServer.start(); // Запуск главного меню
        } else {
            JOptionPane.showMessageDialog(frame, "Второй пароль не совпадает с первым паролем", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }

        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(true); // Включение кнопки
            entranceButton.setText("Изменить"); // Восстановление текста
        });
    }
}
