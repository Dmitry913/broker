package org.wasend.broker.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HelpUtils {

    public static List<String> getWithProtocol(Collection<String> hosts, String protocol, String path) {
        return hosts.stream()
                .map(host -> protocol + host + path)
                .collect(Collectors.toList());
    }
}
