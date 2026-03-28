package cn.bugstack.ai.mcpgateway.infrastructure.adapter.port;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.port.ISessionPort;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SessionPort implements ISessionPort {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SessionPort(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public Object toolCall(McpToolProtocolConfigVO.HTTPConfig httpConfig, Map<String, Object> params) throws IOException {
        Map<String, Object> payload = params == null ? Map.of() : new HashMap<>(params);
        Map<String, Object> headers = parseHeaders(httpConfig.getHttpHeaders());
        String method = httpConfig.getHttpMethod().toLowerCase();

        if ("get".equals(method)) {
            URI uri = buildGetUri(httpConfig.getHttpUrl(), payload);
            return restClient.get()
                    .uri(uri)
                    .headers(httpHeaders -> applyHeaders(httpHeaders, headers))
                    .retrieve()
                    .body(String.class);
        }

        if ("post".equals(method)) {
            Object body = unwrapBody(payload);
            return restClient.post()
                    .uri(httpConfig.getHttpUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> applyHeaders(httpHeaders, headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        }

        throw new AppException(ResponseCode.METHOD_NOT_FOUND.getCode(), "暂不支持的请求方法: " + httpConfig.getHttpMethod());
    }

    private Map<String, Object> parseHeaders(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    private void applyHeaders(HttpHeaders target, Map<String, Object> headers) {
        headers.forEach((key, value) -> target.add(key, String.valueOf(value)));
    }

    private URI buildGetUri(String rawUrl, Map<String, Object> payload) {
        String resolvedUrl = rawUrl;
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(rawUrl);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (payload.containsKey(variable)) {
                resolvedUrl = resolvedUrl.replace("{" + variable + "}", String.valueOf(payload.remove(variable)));
            }
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resolvedUrl);
        payload.forEach(builder::queryParam);
        return builder.build(true).toUri();
    }

    private Object unwrapBody(Map<String, Object> payload) {
        if (payload.size() == 1) {
            Object firstValue = payload.values().iterator().next();
            if (firstValue instanceof Map<?, ?> || firstValue instanceof Iterable<?>) {
                return firstValue;
            }
        }
        return payload;
    }

}
