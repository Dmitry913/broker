package org.wasend.broker.service.mapper;

public interface DefaultMapper<T, E> {
    T mapTo(E e);
}
