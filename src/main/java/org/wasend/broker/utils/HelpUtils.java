package org.wasend.broker.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HelpUtils {

    public static List<String> getWithProtocol(Collection<String> hosts, String protocol, String path) {
        return hosts.stream()
                .map(host -> mapHostToUrl(host, protocol, path))
                .collect(Collectors.toList());
    }

    public static String mapHostToUrl(String host, String protocol, String path) {
        return protocol + "://" + host + path;
    }
}
