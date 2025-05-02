package org.face_recognition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

 // Класс ServerControl представляет графическое приложение для управления сервером
 // Даёт возможность запускать и останавливать сервер через интерфейс с использованием кнопки

public class ServerControl {

    // Поток, в котором работает сервер
    private static Thread serverThread;

    // Флаг, показывающий, запущен ли сервер
    private static boolean isServerRunning = false;

    // Экземпляр сервера
    private static TheServer server;

    // Экземпляр конфигурации
    private static Config config;
    private static JFrame frame;
    private static String password;
    private static Safety safety = new Safety();

    public ServerControl(String password) {
        this.password = password;
    }


    // Основной метод, создающий графическое окно с кнопкой для управления сервером

    static void server_start(){
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
        JLabel label = new JLabel("Запуск сервера");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE); // Белый текст
        label.setFont(new Font("Arial", Font.BOLD, 19)); // Шрифт и стиль
        panel.add(label);

        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Разделитель между кнопками

        // Создаем кнопку для управления сервером
        JButton toggleButton = new JButton("Запустить Сервер");
        customizeButton(toggleButton);

        // Создаем кнопку для управления сервером
        JButton returnEntry = new JButton("Вернуться на панель входа");
        customizeButton(returnEntry);

        // Добавляем обработчик нажатия кнопки
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Проверяем состояние сервера
                if (isServerRunning) {
                    // Если сервер работает, останавливаем его
                    stopServer();
                    toggleButton.setText("Запустить Сервер"); // Меняем текст кнопки
                } else {
                    // Если сервер не работает, запускаем его
                    start();
                    toggleButton.setText("Остановить Сервер"); // Меняем текст кнопки
                }
            }
        });

        // Добавляем обработчик нажатия кнопки
        returnEntry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
                toggleButton.setText("Запустить Сервер"); // Меняем текст кнопки
                frame.dispose();
                EnteringPassword enteringPassword = new EnteringPassword();
                enteringPassword.startEnteringPassword();
            }
        });

        // Добавляем кнопки
        panel.add(toggleButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Разделитель между кнопками
        panel.add(returnEntry);

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


    // Метод для запуска сервера
    // Создает новый поток, где происходит инициализация и запуск сервера

    private static void start() {
        isServerRunning = true; // Устанавливаем флаг, что сервер запущен

        // Создаем поток для запуска сервера
        serverThread = new Thread(() -> {
            try {
                System.out.println("Сервер запускается...");
                // Инициализируем конфигурацию
                config = new Config();
                String[] conf = config.configServer();

                // Создаем и запускаем сервер с параметрами конфигурации
                server = new TheServer(Integer.parseInt(safety.chaha20Decrypt(password,conf[0])),
                        safety.chaha20Decrypt(password,conf[1]), safety.chaha20Decrypt(password,conf[2]),
                        safety.chaha20Decrypt(password,conf[3]), safety.chaha20Decrypt(password,conf[4]), password);
                server.startServer(); // Метод для запуска сервера
            } catch (Exception e) {
                System.out.println("Сервер отключен!"); // Обрабатываем возможные ошибки
            }
        });

        serverThread.start(); // Запускаем поток
    }

    // Метод для остановки сервера
    // Завершает работу сервера и корректно останавливает поток

    private static void stopServer() {
        isServerRunning = false; // Устанавливаем флаг, что сервер остановлен

        if (server != null) {
            try {
                server.stopServer(); // Вызываем метод для завершения работы сервера
            } catch (Exception e) {
                System.err.println("Ошибка при остановке сервера: " + e.getMessage()); // Логгируем ошибки
            }
        }

        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt(); // Прерываем поток сервера
            try {
                serverThread.join(); // Ожидаем завершения потока
            } catch (InterruptedException e) {
                System.err.println("Ошибка при ожидании завершения потока сервера: " + e.getMessage());
            }
        }
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

}
