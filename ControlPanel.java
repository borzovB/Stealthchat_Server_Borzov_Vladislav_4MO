package org.face_recognition;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ControlPanel {

    // Настраивает контекстное меню для поля ввода пароля
    void panalGO(JPasswordField passwordField) {
        JPopupMenu contextMenu = new JPopupMenu(); // Создаем контекстное меню

        // Пункт меню "Копировать"
        JMenuItem copyItem = new JMenuItem("Копировать");
        copyItem.addActionListener(e -> {
            // Копируем текст пароля в буфер обмена
            String passwordText = new String(passwordField.getPassword());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(passwordText), null
            );
        });
        contextMenu.add(copyItem); // Добавляем пункт в меню

        // Пункт меню "Вставить"
        JMenuItem pasteItem = new JMenuItem("Вставить");
        pasteItem.addActionListener(e -> passwordField.paste()); // Выполняем вставку
        contextMenu.add(pasteItem); // Добавляем пункт в меню

        // Обработчик правого клика для показа контекстного меню
        passwordField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Показываем меню при нажатии правой кнопки мыши
                if (e.isPopupTrigger()) {
                    contextMenu.show(passwordField, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Показываем меню при отпускании правой кнопки (для macOS)
                if (e.isPopupTrigger()) {
                    contextMenu.show(passwordField, e.getX(), e.getY());
                }
            }
        });

        // Действие для копирования по Ctrl+C
        Action copyAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Копируем текст пароля в буфер обмена
                String passwordText = new String(passwordField.getPassword());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(passwordText), null
                );
            }
        };

        // Настройка горячих клавиш для поля ввода
        InputMap inputMap = passwordField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = passwordField.getActionMap();

        // Привязываем Ctrl+C к действию копирования
        inputMap.put(KeyStroke.getKeyStroke("ctrl C"), "copy");
        actionMap.put("copy", copyAction);
    }
}
