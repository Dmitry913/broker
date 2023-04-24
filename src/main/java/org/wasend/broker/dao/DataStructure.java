package org.wasend.broker.dao;

public interface DataStructure<T> {

    /**
     *  Добавляет элемент в очередь в корректном порядке. Приоритизирует по времени отправки.
     */
    void add(T elem);

    /**
     * Извлекает элемент из очереди с минимальным значением
     */
    T getMin();
}
