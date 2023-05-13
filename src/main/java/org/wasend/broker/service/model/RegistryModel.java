package org.wasend.broker.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.wasend.broker.dao.entity.MetaInfoZK;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RegistryModel extends Message {
    private LocalDateTime timeRegistration;
    private String url;
    private boolean isReplica;

    @SneakyThrows
    @Override
    public String toString() {
        return String.format("{timeRegistration=%s, url=%s}", timeRegistration.toString(), url);
    }
}
