package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, List<String>> queryParams;

    public Request(String requestLine) {
        final String[] parts = requestLine.split(" ");
        this.method = parts[0];

        final String uri = parts[1];
        final String[] pathParts = uri.split("\\?", 2);
        this.path = pathParts[0];

        this.queryParams = parseQuery(uri);
    }

    private Map<String, List<String>> parseQuery(String uri) {
        final Map<String, List<String>> result = new HashMap<>();
        try {
            List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
            for (NameValuePair param : params) {
                result.computeIfAbsent(param.getName(), k -> new ArrayList<>())
                        .add(param.getValue());
            }
        } catch (Exception e) {
            System.err.println("Error parsing query: " + e.getMessage());
        }
        return Collections.unmodifiableMap(result);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getQueryParam(String name) {
        return queryParams.getOrDefault(name, Collections.emptyList());
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}