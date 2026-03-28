package cn.bugstack.ai.mcpgateway.config;

import cn.bugstack.ai.mcpgateway.api.response.Response;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayAuthDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayAuthPO;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);

    private static final int DEFAULT_API_KEY_LIMIT = 60;
    private static final int CHAT_LIMIT = 10;
    private static final int SSE_LIMIT = 5;
    private static final int TOOL_CALL_LIMIT = 20;
    private static final int GLOBAL_LIMIT = 200;

    private final IMcpGatewayAuthDao mcpGatewayAuthDao;
    private final ObjectMapper objectMapper;
    private final Map<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();

    public ApiRateLimitFilter(IMcpGatewayAuthDao mcpGatewayAuthDao, ObjectMapper objectMapper) {
        this.mcpGatewayAuthDao = mcpGatewayAuthDao;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if ("/api/health".equals(path) || "/debug/mcp/health".equals(path)) {
            return true;
        }
        return !path.startsWith("/custom-mcp/") && !path.startsWith("/debug/mcp/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = CachedBodyHttpServletRequest.from(request);
        String path = wrappedRequest.getRequestURI();
        String apiKey = trimToNull(wrappedRequest.getParameter("api_key"));
        String gatewayId = trimToNull(wrappedRequest.getParameter("gatewayId"));

        if (!allow("global:" + path, GLOBAL_LIMIT, Duration.ofMinutes(1))) {
            log.warn("rate limit blocked by global rule, path={}", path);
            writeRateLimitError(response, "系统繁忙，请稍后重试");
            return;
        }

        if (apiKey != null) {
            int apiKeyLimit = resolveApiKeyLimit(gatewayId, apiKey);
            if (!allow("apiKey:" + apiKey, apiKeyLimit, Duration.ofMinutes(1))) {
                log.warn("rate limit blocked by apiKey rule, gatewayId={}, apiKey={}, limit={}",
                        gatewayId, maskApiKey(apiKey), apiKeyLimit);
                writeRateLimitError(response, "API Key 请求过于频繁");
                return;
            }
        }

        if ("/custom-mcp/sse".equals(path) || path.endsWith("/mcp/sse")) {
            String sseKey = (gatewayId == null ? "-" : gatewayId) + ":" + (apiKey == null ? "anonymous" : apiKey);
            if (!allow("sse:" + sseKey, SSE_LIMIT, Duration.ofMinutes(1))) {
                log.warn("rate limit blocked by sse rule, gatewayId={}, apiKey={}", gatewayId, maskApiKey(apiKey));
                writeRateLimitError(response, "SSE 建连过于频繁");
                return;
            }
        }

        if (path.startsWith("/custom-mcp/chat-with-tools")) {
            String chatKey = (gatewayId == null ? "-" : gatewayId) + ":" + (apiKey == null ? "anonymous" : apiKey);
            if (!allow("chat:" + chatKey, CHAT_LIMIT, Duration.ofMinutes(1))) {
                log.warn("rate limit blocked by chat rule, gatewayId={}, apiKey={}", gatewayId, maskApiKey(apiKey));
                writeRateLimitError(response, "AI 问答请求过于频繁");
                return;
            }
        }

        if ("/custom-mcp/message/result".equals(path)) {
            ToolCallMeta toolCallMeta = extractToolCallMeta(wrappedRequest.getCachedBodyAsString());
            if (toolCallMeta.isToolCall()) {
                String toolKey = (gatewayId == null ? "-" : gatewayId) + ":" + (apiKey == null ? "anonymous" : apiKey) + ":" + toolCallMeta.toolName();
                if (!allow("tool:" + toolKey, TOOL_CALL_LIMIT, Duration.ofMinutes(1))) {
                    log.warn("rate limit blocked by tool rule, gatewayId={}, toolName={}, apiKey={}",
                            gatewayId, toolCallMeta.toolName(), maskApiKey(apiKey));
                    writeRateLimitError(response, "工具调用过于频繁");
                    return;
                }
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private int resolveApiKeyLimit(String gatewayId, String apiKey) {
        if (apiKey == null) {
            return DEFAULT_API_KEY_LIMIT;
        }

        McpGatewayAuthPO req = new McpGatewayAuthPO();
        req.setGatewayId(gatewayId);
        req.setApiKey(apiKey);
        McpGatewayAuthPO auth = mcpGatewayAuthDao.queryMcpGatewayAuthPO(req);
        if (auth == null || auth.getRateLimit() == null || auth.getRateLimit() <= 0) {
            return DEFAULT_API_KEY_LIMIT;
        }
        return auth.getRateLimit();
    }

    private ToolCallMeta extractToolCallMeta(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String method = root.path("method").asText("");
            if (!"tools/call".equals(method)) {
                return ToolCallMeta.none();
            }
            String toolName = root.path("params").path("name").asText("");
            if (toolName.isBlank()) {
                return ToolCallMeta.none();
            }
            return new ToolCallMeta(true, toolName);
        } catch (Exception e) {
            return ToolCallMeta.none();
        }
    }

    private boolean allow(String key, int limit, Duration window) {
        FixedWindowCounter counter = counters.computeIfAbsent(key, ignored -> new FixedWindowCounter(window));
        return counter.tryAcquire(limit);
    }

    private void writeRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Response.error(ResponseCode.RATE_LIMIT_FAIL.getCode(), message)));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return apiKey == null ? "-" : "***";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private record ToolCallMeta(boolean toolCall, String toolName) {
        static ToolCallMeta none() {
            return new ToolCallMeta(false, "");
        }

        boolean isToolCall() {
            return toolCall;
        }
    }

    private static final class FixedWindowCounter {
        private final Duration window;
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant windowStart = Instant.now();

        private FixedWindowCounter(Duration window) {
            this.window = window;
        }

        private synchronized boolean tryAcquire(int limit) {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plus(window))) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
