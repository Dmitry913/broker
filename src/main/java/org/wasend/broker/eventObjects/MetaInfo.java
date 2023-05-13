package org.wasend.broker.eventObjects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.wasend.broker.dao.entity.MetaInfoZK;

@Getter
public class MetaInfo extends ApplicationEvent {
    private int rootDirectoryVersion;
    private MetaInfoZK metaInfoZK;

    public MetaInfo(Object source, int rootDirectoryVersion, MetaInfoZK metaInfoZK) {
        super(source);
        this.rootDirectoryVersion = rootDirectoryVersion;
        this.metaInfoZK = metaInfoZK;
    }
}
