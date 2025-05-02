package org.face_recognition;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

 //Класс ChangeLoginPassword предоставляет графический интерфейс для ввода текущего пароля
 //перед изменением пароля пользователя. Используется для проверки или регистрации пароля
 //с применением Argon2 для хэширования и ChaCha20 для шифрования конфигурационных данных.

public class ChangeLoginPassword {

    // Константа для имени файла конфигурации пользователя
    private static final String CONFIG_FILE_NAME = "config_user.properties";
    // Константа для имени файла конфигурации сервера
    private static final String CONFIG_FILE_NAME_SERVER = "config.properties";
    // Объект для работы с конфигурационными файлами
    private static Config config = new Config();
    // Главное окно приложения
    private static JFrame frame;
    // Объект для операций шифрования (предположительно содержит метод Argon2)
    private static Encryption encryption = new Encryption();
    // Панель для текстового поля пароля и кнопки "Показать пароль"
    private static JPanel textPanel;
    // Объект Argon2 для хэширования и проверки паролей
    private static Argon2 argon2 = Argon2Factory.create();
    // Объект для возврата в главное меню приложения
    private static StartServer startServer = new StartServer();
    // Объект для валидации пароля и шифрования данных
    private static Safety safety = new Safety();


    //Запускает графический интерфейс для ввода текущего пароля.
    //Создает окно с полем для пароля, кнопкой для его отображения и кнопками управления.

    public static void startEnteringPassword(){
        frame = new JFrame("Управление Сервером");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400); // Начальный размер окна
        frame.setLocationRelativeTo(null); // Центрируем окно на экране

        // Устанавливаем минимальные размеры окна
        Dimension minSize = new Dimension(540, 430);
        frame.setMinimumSize(minSize);

        // Создаём главную панель с BoxLayout для вертикального центрирования
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); // Вертикальная компоновка
        mainPanel.setBackground(new Color(30, 30, 30)); // Фон для основной панели
        mainPanel.setOpaque(true); // Убедимся, что фон отображается

        // Добавляем пустое пространство сверху для центрирования панели
        mainPanel.add(Box.createVerticalGlue()); // Пустое пространство сверху

        // Создаём панель с кнопками
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Вертикальная компоновка для кнопок
        panel.setBackground(new Color(60, 63, 65)); // Устанавливаем тёмный фон

        // Устанавливаем фиксированные размеры для panel
        panel.setPreferredSize(new Dimension(400, 300)); // Размеры панели с кнопками
        panel.setMinimumSize(new Dimension(400, 300));  // Минимальные размеры для панели
        panel.setMaximumSize(new Dimension(400, 300));  // Максимальные размеры для панели

        // Добавляем отступы для панели
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(Box.createRigidArea(new Dimension(0, 40)));

        // Добавляем заголовок
        JLabel label = new JLabel("Осуществление входа");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE); // Белый текст
        label.setFont(new Font("Arial", Font.BOLD, 21)); // Шрифт и стиль
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Добавляем пустое пространство

        // Создаем панель для текстового поля и кнопки "Показать пароль", чтобы добавить отступы
        textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS)); // Горизонтальная компоновка

        // Кнопка для временного отображения пароля
        JButton showPasswordButton = new JButton();

        JButton showStartButton = new JButton("Вернуться в главное меню");
        customizeButton(showStartButton);

        showStartButton.addActionListener(e -> {
            frame.dispose();
            startServer.start();
        });

        // Загружаем иконки
        ImageIcon originalIconNot = new ImageIcon("eye_icon.png"); // Иконка для "показать"
        ImageIcon originalIcon = new ImageIcon("eye_icon_not.png"); // Иконка для "скрыть"

        // Масштабируем иконки
        Image scaledImg = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImg); // Масштабированная иконка для "показать"

        Image scaledImgNot = originalIconNot.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIconNot = new ImageIcon(scaledImgNot); // Масштабированная иконка для "скрыть"

        // Устанавливаем начальную иконку в кнопку
        showPasswordButton.setIcon(scaledIconNot); // Начальное состояние — пароль скрыт
        showPasswordButton.setContentAreaFilled(false); // Убираем фон кнопки
        showPasswordButton.setBorder(BorderFactory.createEmptyBorder()); // Убираем рамку

        // Сделаем кнопку квадратной (с одинаковой высотой и шириной)
        showPasswordButton.setPreferredSize(new Dimension(40, 40)); // Размеры кнопки
        showPasswordButton.setMaximumSize(new Dimension(40, 40)); // Максимальный размер кнопки
        showPasswordButton.setMinimumSize(new Dimension(40, 40)); // Минимальный размер кнопки

        // Поле для пароля
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(250, 40)); // Размер текстового поля
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16)); // Шрифт текстового поля

        // Флаг для отслеживания состояния пароля
        final boolean[] isPasswordVisible = {false}; // По умолчанию пароль скрыт

        // Таймер для автоматического скрытия пароля
        Timer hidePasswordTimer = new Timer(10000, event -> {
            passwordField.setEchoChar('•'); // Скрываем пароль снова
            isPasswordVisible[0] = false;  // Обновляем состояние
            showPasswordButton.setIcon(scaledIconNot); // Устанавливаем иконку "скрыть"
        });

        // Настраиваем кнопку "Показать пароль"
        showPasswordButton.addActionListener(e -> {
            if (isPasswordVisible[0]) {
                // Если пароль уже виден, скрываем его
                passwordField.setEchoChar('•'); // Скрываем пароль
                isPasswordVisible[0] = false;  // Обновляем состояние
                hidePasswordTimer.stop();      // Останавливаем таймер
                showPasswordButton.setIcon(scaledIconNot); // Меняем иконку на "скрыть"
            } else {
                // Если пароль скрыт, показываем его
                passwordField.setEchoChar((char) 0); // Показываем пароль
                isPasswordVisible[0] = true;  // Обновляем состояние

                // Перезапускаем таймер для автоматического скрытия
                hidePasswordTimer.restart();
                showPasswordButton.setIcon(scaledIcon); // Меняем иконку на "показать"
            }
        });
        ControlPanel controlPanel = new ControlPanel();
        controlPanel.panalGO(passwordField);
        // Добавляем кнопку и поле для пароля в панель
        textPanel.add(showPasswordButton);
        textPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ слева от поля пароля
        textPanel.add(passwordField);
        textPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Отступ справа от поля пароля

        panel.add(textPanel); // Добавляем панель с текстовым полем и кнопкой на основную панель

        panel.add(Box.createRigidArea(new Dimension(0, 30))); // Разделитель между кнопками

        // Кнопка "Вход"
        JButton entrance = new JButton("Вход");
        customizeButton(entrance);
        entrance.addActionListener(e -> {
            // Получаем введённый пароль
            char[] password = passwordField.getPassword();

            // Создаем и запускаем новый поток
            new Thread(() -> handleEntrance(entrance, new String(password))).start();
        });

        // Добавляем кнопки
        panel.add(entrance);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Разделитель между кнопками
        panel.add(showStartButton);

        panel.add(Box.createRigidArea(new Dimension(0, 80))); // Разделитель между кнопками
        // Устанавливаем светло-серая рамка
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 10)); // Светло-серая рамка толщиной 10

        // Добавляем панель с кнопками в основную панель
        mainPanel.add(panel); // Добавляем панель по центру

        // Добавляем пустое пространство снизу для центрирования панели
        mainPanel.add(Box.createVerticalGlue()); // Пустое пространство снизу

        // Устанавливаем mainPanel как содержимое JFrame
        frame.setContentPane(mainPanel); // Используем setContentPane для установки mainPanel

        frame.setVisible(true);
    }

    // Метод для кастомизации кнопок
    private static void customizeButton(JButton button) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(new Color(75, 110, 175)); // Синий фон
        button.setForeground(Color.WHITE); // Белый текст
        button.setFont(new Font("Arial", Font.BOLD, 16)); // Шрифт
        button.setFocusPainted(false); // Убираем обводку при фокусе
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Отступы внутри кнопки
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Курсор "рука"
        // Устанавливаем размеры кнопки
        button.setPreferredSize(new Dimension(250, 60)); // Устанавливаем предпочтительный размер
        button.setMinimumSize(new Dimension(150, 40));  // Минимальный размер
        button.setMaximumSize(new Dimension(250, 40));  // Максимальный размер
    }

    // Обработка нажатия на кнопку "Вход"
    private static void handleEntrance(JButton entranceButton, String password) {
        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(false);  // Отключаем кнопку
            entranceButton.setText("Вход...");  // Меняем текст кнопки на "Вход..."
        });

        if (!password.isEmpty()){

            if (!safety.isValidPassword(password)) {
                JOptionPane.showMessageDialog(frame, "Пароль должен содержать не менее 8 символов, " +
                        "включая строчные и прописные буквы и цифры.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }else {
                System.out.println("Начата обработка входа...");
                try {
                    if (!isConfigFilePresent()) {

                        int result = JOptionPane.showConfirmDialog(frame, "Хотите пройти регистрацию вашего пароля при первом входе?", "Подтверждение", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            config.createConfigFile(CONFIG_FILE_NAME);
                            config.setConfigProperty("db.password", encryption.Argon2(password), CONFIG_FILE_NAME);
                            String conf_str [] = safety.configMass();
                            int i = 0;

                            while(i<conf_str.length){
                                String confOld = config.getProperty(conf_str[i], CONFIG_FILE_NAME_SERVER);
                                String srt = safety.chaha20Encript(password, confOld);
                                config.setConfigProperty(conf_str[i], srt, CONFIG_FILE_NAME_SERVER);
                                i++;
                            }
                            JOptionPane.showMessageDialog(frame, "Действие выполнено.", "Результат", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(frame, "Действие отменено.", "Результат", JOptionPane.WARNING_MESSAGE);
                        }

                    }else {

                        String dbPassword = config.getProperty("db.password", CONFIG_FILE_NAME);
                        // Проверяем имя и пароль с использованием Argon2
                        if (argon2.verify(dbPassword, password.toCharArray())) {
                            PasswordChangePanel passwordChangePanel = new PasswordChangePanel(password);
                            JOptionPane.showMessageDialog(frame, "Операция входа завершена успешно!", "Информация", JOptionPane.INFORMATION_MESSAGE);
                            passwordChangePanel.startChangePassword();
                            frame.dispose();
                        }else {
                            JOptionPane.showMessageDialog(frame, "Неверный пароль!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }

                    }
                } catch (IOException e) {
                    System.err.println("An error occurred: " + e.getMessage());
                    JOptionPane.showMessageDialog(frame, "An error occurred.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Обработка входа завершена.");
            }

        }else {
            JOptionPane.showMessageDialog(frame, "Поле должно быть заполнено!.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }

        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(true);  // Включаем кнопку обратно
            entranceButton.setText("Вход");  // Возвращаем исходный текст кнопки
        });
    }

    // Проверяет наличие файла config.properties в корне проекта
    public static boolean isConfigFilePresent() {
        Path path = Paths.get(CONFIG_FILE_NAME);
        return Files.exists(path);
    }
}
