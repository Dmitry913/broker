package org.wasend.broker.eventObjects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.wasend.broker.dao.entity.MetaInfoZK;

@AllArgsConstructor
@Getter
public class MetaInfo {
    private int rootDirectoryVersion;
    private MetaInfoZK metaInfoZK;
}
