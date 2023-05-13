package org.wasend.broker.eventObjects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Children {
    private final List<String> ephemeralChildrenNodes;
}
