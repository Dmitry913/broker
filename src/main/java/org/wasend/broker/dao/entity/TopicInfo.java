package org.wasend.broker.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class TopicInfo {
    /** Наименование данного топика*/
    private String name;
    /** Название(наименование директории) мастер-узла для каждой партиции*/
    private Set<String> partitionsId;
//    /** Идентификаторы узлов, в которых хранятся, копии данной реплики */
//    private Set<String> replicasPlace;
//    /** Идентификатор мастер-узла*/
//    private String masterNodeName;
}
