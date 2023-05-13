package org.wasend.broker.service.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// todo сделать универсальный маппер
@Component
public class MapperFactory {

    private static final String DEFAULT_MAPPER_METHOD_NAME = "mapTo";
    // мапа: <ReturnType, <SourceType, Mapper>>
    private final Map<Class, Map<Class, DefaultMapper>> allMappers;

    @Autowired
    public MapperFactory(List<DefaultMapper> mappers) {
        allMappers = new HashMap<>();
        for (DefaultMapper mapper : mappers) {
            Arrays.stream(mapper.getClass().getMethods())
                    .filter(m -> m.getName().equals(DEFAULT_MAPPER_METHOD_NAME))
                    .forEach(m -> {
                        Class returnType = m.getReturnType();
                        Class sourceType = m.getParameterTypes()[0];
                        Map<Class, DefaultMapper> sourceToMapper = new HashMap<>();
                        sourceToMapper.put(sourceType, mapper);
                        Map<Class, DefaultMapper> existValue = allMappers.putIfAbsent(returnType, sourceToMapper);
                        if (existValue != null) {
                            existValue.put(sourceType, mapper);
                        }
                    });
        }
    }

    public <T,E> E mapTo(T from, Class<T> classFromMap, Class<E> classToMap) {
        if (from == null) {
            return null;
        }
        Map<Class, DefaultMapper> sourceToMapper = allMappers.get(classToMap);
        if (sourceToMapper == null) {
            return null;
        }
        DefaultMapper mapper = sourceToMapper.get(classFromMap);
        if (mapper == null) {
            return null;
        }
        return (E) mapper.mapTo(from);
    }

}
