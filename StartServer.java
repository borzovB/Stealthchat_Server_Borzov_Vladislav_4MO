package org.face_recognition;

import javax.swing.*;
import java.awt.*;

 //Класс StartServer предоставляет графический интерфейс для начального управления серверным приложением.
 //Основные функции:
 //- Создание окна с кнопками "Вход" и "Изменение пароля".
 //- Переход к интерфейсам ввода пароля (EnteringPassword) или изменения пароля (ChangeLoginPassword).
 //- Обработка действий кнопок в отдельных потоках для предотвращения блокировки UI.

public class StartServer {

    private static EnteringPassword password = new EnteringPassword(); // Объект для перехода к интерфейсу ввода пароля
    private static JFrame frame; // Главное окно приложения

    public static void main(String[] args){
        start();
    }

    public static void start(){
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

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Добавляем заголовок
        JLabel label = new JLabel("Управление сервером");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE); // Белый текст
        label.setFont(new Font("Arial", Font.BOLD, 19)); // Шрифт и стиль
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Добавляем пустое пространство

        // Кнопка "Вход"
        JButton entrance = new JButton("Вход");
        customizeButton(entrance);
        entrance.addActionListener(e -> {
            // Создаем и запускаем новый поток
            new Thread(() -> handleEntrance(entrance)).start();
        });

        // Кнопка "Изменение пароля"
        JButton passwordRecovery = new JButton("Изменение пароля");
        customizeButton(passwordRecovery);
        passwordRecovery.addActionListener(e -> {
            // Создаем и запускаем новый поток
            new Thread(() -> handlePasswordRecovery(passwordRecovery)).start();
        });

        // Добавляем кнопки
        panel.add(entrance);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Разделитель между кнопками
        panel.add(passwordRecovery);

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
    private static void handleEntrance(JButton entranceButton) {
        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(false);  // Отключаем кнопку
            entranceButton.setText("Вход...");  // Меняем текст кнопки на "Вход..."
        });

        frame.dispose();
        password.startEnteringPassword();

        SwingUtilities.invokeLater(() -> {
            entranceButton.setEnabled(true);  // Включаем кнопку обратно
            entranceButton.setText("Вход");  // Возвращаем исходный текст кнопки
        });
    }

    // Обработка нажатия на кнопку "Изменение пароля"
    private static void handlePasswordRecovery(JButton passwordRecoveryButton) {
        SwingUtilities.invokeLater(() -> {
            passwordRecoveryButton.setEnabled(false);  // Отключаем кнопку
            passwordRecoveryButton.setText("Изменение пароля...");  // Меняем текст кнопки на "Изменение пароля..."
        });

        ChangeLoginPassword changeLoginPassword = new ChangeLoginPassword();
        frame.dispose();
        changeLoginPassword.startEnteringPassword();

        SwingUtilities.invokeLater(() -> {
            passwordRecoveryButton.setEnabled(true);  // Включаем кнопку обратно
            passwordRecoveryButton.setText("Изменение пароля");  // Возвращаем исходный текст кнопки
        });
    }
}
