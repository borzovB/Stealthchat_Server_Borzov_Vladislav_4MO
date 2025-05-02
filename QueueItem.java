package org.face_recognition;

// Класс QueueItem представляет элемент очереди, используемой для асинхронной передачи данных (сообщений или файлов)
// в серверном приложении. Реализует интерфейсы Comparable и Serializable для поддержки сортировки элементов в
// приоритетной очереди (PriorityBlockingQueue) и сериализации объектов для передачи по сети
// Основные функции:
// - Хранение данных (сообщений или файлов) и их приоритета
// - Обеспечение упорядочивания элементов в очереди на основе приоритета и порядкового номера
// - Генерация уникальных порядковых номеров для сохранения порядка вставки при равных приоритетах

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

class QueueItem implements Comparable<QueueItem>, Serializable {
    private final Object data; // Данные элемента (например, строка сообщения или объект FileTransferData)
    private final int priority; // Приоритет элемента: 0 для сообщений, 1 для файлов
    private final long sequenceNumber; // Уникальный порядковый номер для сохранения порядка вставки
    private static final AtomicLong sequenceGenerator = new AtomicLong(0); // Генератор уникальных номеров, атомарный для потокобезопасности

    // Конструктор QueueItem
    // Инициализирует элемент очереди с указанными данными и приоритетом
    // Генерирует уникальный порядковый номер для элемента

    public QueueItem(Object data, int priority) {
        this.data = data; // Сохранение данных
        this.priority = priority; // Установка приоритета
        this.sequenceNumber = sequenceGenerator.getAndIncrement(); // Получение и инкремент уникального номера
    }

    // Возвращает данные, хранящиеся в элементе очереди

    public Object getData() {
        return data;
    }

    // Сравнивает текущий элемент с другим элементом очереди для определения порядка в PriorityBlockingQueue
    // - Сначала сравнивает элементы по приоритету (меньший приоритет обрабатывается раньше)
    // - Если приоритеты равны, сравнивает по порядковому номеру (меньший номер обрабатывается раньше)

    @Override
    public int compareTo(QueueItem other) {
        // Сравнение по приоритету (меньший приоритет имеет более высокий приоритет в очереди)
        int priorityComparison = Integer.compare(this.priority, other.priority);
        if (priorityComparison != 0) {
            return priorityComparison; // Возврат результата, если приоритеты различаются
        }
        // Сравнение по порядковому номеру при равных приоритетах
        return Long.compare(this.sequenceNumber, other.sequenceNumber);
    }
}
