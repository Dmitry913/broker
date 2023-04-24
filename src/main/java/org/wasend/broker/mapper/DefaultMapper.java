package org.wasend.broker.mapper;

public interface DefaultMapper<T, E> {
    T mapTo(E e);
}
