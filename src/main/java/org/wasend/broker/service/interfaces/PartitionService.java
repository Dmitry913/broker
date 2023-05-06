package org.wasend.broker.service.interfaces;

import java.util.Set;

/**
 * Интерфейс, который позволяет выбрать реплики, при создании нового топика.
 */
// todo можно было бы перенести логику по распределению нагрузки из сервиса MessageService так же в этот сервис
public interface PartitionService {

    Set<String> getNodesForNewTopic();

}
