package org.wasend.broker.service.interfaces;

import java.util.Set;

/**
 * Сервис для балансировки нагрузки.
 */
// todo можно было бы перенести логику по распределению нагрузки из сервиса MessageService так же в этот сервис
public interface BalancerPartitionService {

    /**
     * Позволяет выбрать реплики, при создании нового топика.
     * @return наименее загруженные узлы (название их директорий).
     */
    Set<String> getDirectoryNodesForNewTopic();

}
