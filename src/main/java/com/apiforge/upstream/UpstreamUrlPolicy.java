package com.apiforge.upstream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UpstreamUrlPolicy {

    private final Set<String> allowedHosts;

    public UpstreamUrlPolicy(@Value("${apiforge.gateway.allowed-hosts:}") String configuredHosts) {
        this.allowedHosts = Arrays.stream(configuredHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String validateAndNormalize(String value) {
        if (value == null || value.isBlank()) {
            throw invalidDestination();
        }

        final URI uri;
        try {
            uri = URI.create(value.trim()).normalize();
        } catch (IllegalArgumentException exception) {
            throw invalidDestination();
        }

        String host = uri.getHost();
        if (!uri.isAbsolute()
                || !"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || !uri.toString().equals(value.trim())) {
            throw invalidDestination();
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (isUnsafeHost(normalizedHost) || !allowedHosts.contains(normalizedHost)) {
            throw invalidDestination();
        }

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (path.contains("\\\\") || path.contains("%2e") || path.contains("%2E")) {
            throw invalidDestination();
        }

        String normalized = "https://" + normalizedHost
                + (uri.getPort() == -1 ? "" : ":443") + path;
        while (normalized.endsWith("/") && normalized.length() > "https://x".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isUnsafeHost(String host) {
        return host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || host.endsWith(".test")
                || host.endsWith(".invalid")
                || host.startsWith("127.")
                || host.equals("0.0.0.0")
                || host.equals("::1")
                || host.contains(":")
                || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private ResponseStatusException invalidDestination() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Upstream URL must use HTTPS and an explicitly allowlisted public hostname");
    }
}
