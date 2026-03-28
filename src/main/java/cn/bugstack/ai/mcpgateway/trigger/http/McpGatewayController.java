package cn.bugstack.ai.mcpgateway.trigger.http;

import cn.bugstack.ai.mcpgateway.api.response.Response;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpMessageService;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpSessionService;
import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionMessageService;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class McpGatewayController {

    private final IMcpSessionService mcpSessionService;
    private final IMcpMessageService mcpMessageService;
    private final ISessionManagementService sessionManagementService;
    private final ISessionMessageService sessionMessageService;
    private final ISessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public McpGatewayController(
            IMcpSessionService mcpSessionService,
            IMcpMessageService mcpMessageService,
            ISessionManagementService sessionManagementService,
            ISessionMessageService sessionMessageService,
            ISessionRepository sessionRepository,
            ObjectMapper objectMapper) {
        this.mcpSessionService = mcpSessionService;
        this.mcpMessageService = mcpMessageService;
        this.sessionManagementService = sessionManagementService;
        this.sessionMessageService = sessionMessageService;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "{gatewayId}/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleSseConnection(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        validateGatewayId(gatewayId);
        return mcpSessionService.createMcpSession(gatewayId, apiKey);
    }

    @GetMapping(value = "custom-mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleCustomSseConnection(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        return handleSseConnection(gatewayId, apiKey);
    }

    @PostMapping(value = "{gatewayId}/mcp/sse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleMessage(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        try {
            validateGatewayId(gatewayId);
            if (sessionId == null || sessionId.isBlank()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "sessionId 不能为空");
            }

            HandleMessageCommandEntity commandEntity = new HandleMessageCommandEntity(gatewayId, apiKey, sessionId, messageBody);
            return Mono.just(mcpMessageService.handleMessage(commandEntity));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    @PostMapping(value = "custom-mcp/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleCustomMessage(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        return handleMessage(gatewayId, sessionId, apiKey, messageBody);
    }

    @PostMapping(value = "custom-mcp/message/result", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleCustomMessageWithResult(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        return debugHandleMessage(gatewayId, sessionId, apiKey, messageBody);
    }

    @PostMapping(value = "custom-mcp/chat-with-tools", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatWithTools(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody Map<String, Object> requestBody) {
        try {
            validateGatewayId(gatewayId);
            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "sessionId 不能为空"));
            }

            SessionConfigVO session = sessionManagementService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            String aiBaseUrl = stringValue(requestBody.get("aiBaseUrl"));
            String aiApiKey = stringValue(requestBody.get("aiApiKey"));
            String aiModel = stringValue(requestBody.get("aiModel"));
            String userQuestion = stringValue(requestBody.get("userQuestion"));
            String systemPrompt = stringValue(requestBody.get("systemPrompt"));
            boolean enableMcpTools = booleanValue(requestBody.get("enableMcpTools"), true);

            if (isBlank(aiBaseUrl) || isBlank(aiApiKey) || isBlank(aiModel) || isBlank(userQuestion)) {
                return ResponseEntity.badRequest().body(Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "aiBaseUrl、aiApiKey、aiModel、userQuestion 不能为空"));
            }

            ArrayNode messages = objectMapper.createArrayNode();
            if (!isBlank(systemPrompt)) {
                messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt));
            }
            messages.add(objectMapper.createObjectNode().put("role", "user").put("content", userQuestion));

            List<Map<String, Object>> mcpTools = new ArrayList<>();
            ArrayNode openAiTools = objectMapper.createArrayNode();
            if (enableMcpTools) {
                McpSchemaVO.JSONRPCResponse toolsResponse = processMessageForResult(gatewayId, sessionId, apiKey, buildJsonRpcRequest("tools/list", Map.of()));
                mcpTools = extractTools(toolsResponse);
                openAiTools = buildOpenAiTools(mcpTools);
            }

            JsonNode firstAiResponse = invokeOpenAiCompatible(aiBaseUrl, aiApiKey, aiModel, messages, openAiTools.isEmpty() ? null : openAiTools);
            JsonNode firstMessage = firstAiResponse.path("choices").path(0).path("message");
            if (firstMessage.isMissingNode() || firstMessage.isNull()) {
                return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), "AI 没有返回有效消息"));
            }

            messages.add(firstMessage);
            ArrayNode toolCalls = arrayNodeOf(firstMessage.path("tool_calls"));
            List<Map<String, Object>> toolExecutions = new ArrayList<>();
            for (JsonNode toolCall : toolCalls) {
                String toolName = toolCall.path("function").path("name").asText();
                JsonNode argumentsNode = parseToolArguments(toolCall.path("function").path("arguments").asText("{}"));
                McpSchemaVO.JSONRPCResponse toolResult = processMessageForResult(
                        gatewayId,
                        sessionId,
                        apiKey,
                        buildJsonRpcRequest("tools/call", Map.of(
                                "name", toolName,
                                "arguments", objectMapper.convertValue(argumentsNode, Object.class))));

                toolExecutions.add(Map.of(
                        "toolName", toolName,
                        "arguments", objectMapper.convertValue(argumentsNode, Object.class),
                        "result", toolResult.result() != null ? toolResult.result() : toolResult.error()));

                messages.add(objectMapper.createObjectNode()
                        .put("role", "tool")
                        .put("tool_call_id", toolCall.path("id").asText())
                        .put("content", objectMapper.writeValueAsString(toolResult.result() != null ? toolResult.result() : toolResult.error())));
            }

            JsonNode finalAiResponse = toolCalls.isEmpty() ? firstAiResponse : invokeOpenAiCompatible(aiBaseUrl, aiApiKey, aiModel, messages, null);
            JsonNode finalMessage = finalAiResponse.path("choices").path(0).path("message");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gatewayId", gatewayId);
            result.put("sessionId", sessionId);
            result.put("usedMcpTools", enableMcpTools);
            result.put("toolCount", mcpTools.size());
            result.put("toolExecutions", toolExecutions);
            result.put("answer", extractMessageText(finalMessage));
            result.put("firstModelResponse", firstAiResponse);
            if (!toolCalls.isEmpty()) {
                result.put("finalModelResponse", finalAiResponse);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/tool/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerTool(@RequestBody ToolRegisterRequest request) {
        try {
            validateToolRegisterRequest(request);

            long toolId = nextId();
            McpToolConfigVO saved = sessionRepository.saveMcpToolConfig(new McpToolConfigVO(
                    request.getGatewayId(),
                    toolId,
                    request.getToolName(),
                    request.getToolDescription(),
                    isBlank(request.getToolVersion()) ? "1.0.0" : request.getToolVersion(),
                    new McpToolProtocolConfigVO(
                            new McpToolProtocolConfigVO.HTTPConfig(
                                    request.getHttpUrl(),
                                    isBlank(request.getHttpHeaders()) ? "{}" : request.getHttpHeaders(),
                                    request.getHttpMethod().toUpperCase(),
                                    request.getTimeout() == null ? 30000 : request.getTimeout()),
                            buildMappings(request.getMappings()))));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gatewayId", saved.getGatewayId());
            result.put("toolId", saved.getToolId());
            result.put("toolName", saved.getToolName());
            result.put("mappingCount", request.getMappings().size());
            result.put("status", "CREATED");
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "custom-mcp/tool/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryToolList(@RequestParam("gatewayId") String gatewayId) {
        try {
            validateGatewayId(gatewayId);
            return ResponseEntity.ok(sessionRepository.queryToolDetailsByGatewayId(gatewayId));
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "debug/mcp/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "mcp-gateway");
        result.put("protocol", "MCP over SSE");
        result.put("timestamp", Instant.now().toString());
        result.put("defaultGatewayId", "gateway_001");
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "debug/mcp/{gatewayId}/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            @PathVariable("gatewayId") String gatewayId,
            @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        SessionConfigVO session = sessionManagementService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildSessionPayload(gatewayId, session, apiKey));
    }

    @GetMapping(value = "custom-mcp/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCustomSessionInfo(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        return getSessionInfo(gatewayId, sessionId, apiKey);
    }

    @DeleteMapping(value = "debug/mcp/{gatewayId}/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> disconnectSession(
            @PathVariable("gatewayId") String gatewayId,
            @PathVariable("sessionId") String sessionId) {
        SessionConfigVO session = sessionManagementService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        sessionManagementService.removeSession(sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("sessionId", sessionId);
        result.put("disconnected", true);
        result.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping(value = "custom-mcp/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> disconnectCustomSession(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId) {
        return disconnectSession(gatewayId, sessionId);
    }

    @PostMapping(value = "debug/mcp/{gatewayId}/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> debugHandleMessage(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        try {
            SessionConfigVO session = sessionManagementService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            HandleMessageCommandEntity commandEntity = new HandleMessageCommandEntity(gatewayId, apiKey, sessionId, messageBody);
            McpSchemaVO.JSONRPCResponse response = sessionMessageService.processHandlerMessage(commandEntity);
            if (response == null) {
                return ResponseEntity.accepted().build();
            }

            session.getSink().tryEmitNext(ServerSentEvent.<String>builder()
                    .event("message")
                    .data(objectMapper.writeValueAsString(response))
                    .build());
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    private void validateGatewayId(String gatewayId) {
        if (isBlank(gatewayId)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "gatewayId 不能为空");
        }
    }

    private Map<String, Object> buildSessionPayload(String gatewayId, SessionConfigVO session, String apiKey) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("sessionId", session.getSessionId());
        result.put("active", session.isActive());
        result.put("createTime", session.getCreateTime().toString());
        result.put("lastAccessedTime", session.getLastAccessedTime().toString());

        String messageEndpoint = "/custom-mcp/message?gatewayId=" + gatewayId + "&sessionId=" + session.getSessionId();
        if (!isBlank(apiKey)) {
            messageEndpoint += "&api_key=" + apiKey;
        }
        result.put("messageEndpoint", messageEndpoint);
        result.put("sseEndpoint", "/custom-mcp/sse?gatewayId=" + gatewayId + (isBlank(apiKey) ? "" : "&api_key=" + apiKey));
        return result;
    }

    private McpSchemaVO.JSONRPCResponse processMessageForResult(String gatewayId, String sessionId, String apiKey, String messageBody) throws Exception {
        HandleMessageCommandEntity commandEntity = new HandleMessageCommandEntity(gatewayId, apiKey, sessionId, messageBody);
        return sessionMessageService.processHandlerMessage(commandEntity);
    }

    private String buildJsonRpcRequest(String method, Map<String, Object> params) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.put("id", UUID.randomUUID().toString());
        root.put("method", method);
        root.set("params", objectMapper.valueToTree(params));
        return objectMapper.writeValueAsString(root);
    }

    private List<Map<String, Object>> extractTools(McpSchemaVO.JSONRPCResponse response) {
        if (response == null || response.result() == null) {
            return List.of();
        }
        Map<String, Object> result = objectMapper.convertValue(response.result(), Map.class);
        Object tools = result.get("tools");
        if (!(tools instanceof List<?> toolList)) {
            return List.of();
        }
        List<Map<String, Object>> parsed = new ArrayList<>();
        for (Object item : toolList) {
            parsed.add(objectMapper.convertValue(item, Map.class));
        }
        return parsed;
    }

    private ArrayNode buildOpenAiTools(List<Map<String, Object>> tools) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Map<String, Object> tool : tools) {
            ObjectNode toolNode = objectMapper.createObjectNode();
            ObjectNode functionNode = objectMapper.createObjectNode();
            functionNode.put("name", stringValue(tool.get("name")));
            functionNode.put("description", stringValue(tool.get("description")));
            functionNode.set("parameters", objectMapper.valueToTree(tool.getOrDefault("inputSchema", Map.of("type", "object", "properties", Map.of()))));
            toolNode.put("type", "function");
            toolNode.set("function", functionNode);
            arrayNode.add(toolNode);
        }
        return arrayNode;
    }

    private JsonNode invokeOpenAiCompatible(String aiBaseUrl, String aiApiKey, String aiModel, ArrayNode messages, ArrayNode tools) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", aiModel);
        payload.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            payload.set("tools", tools);
            payload.put("tool_choice", "auto");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(aiBaseUrl) + "/chat/completions"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "AI 接口调用失败: " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private ArrayNode arrayNodeOf(JsonNode node) {
        if (node instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        return objectMapper.createArrayNode();
    }

    private JsonNode parseToolArguments(String rawArguments) {
        try {
            return objectMapper.readTree(isBlank(rawArguments) ? "{}" : rawArguments);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String extractMessageText(JsonNode message) {
        if (message == null || message.isMissingNode() || message.isNull()) {
            return "";
        }
        JsonNode contentNode = message.path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText())) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return message.toString();
    }

    private List<McpToolProtocolConfigVO.ProtocolMapping> buildMappings(List<ToolMappingRequest> mappings) {
        List<McpToolProtocolConfigVO.ProtocolMapping> result = new ArrayList<>();
        int sortOrder = 1;
        for (ToolMappingRequest mapping : mappings) {
            result.add(new McpToolProtocolConfigVO.ProtocolMapping(
                    isBlank(mapping.getMappingType()) ? "request" : mapping.getMappingType(),
                    trimToNull(mapping.getParentPath()),
                    mapping.getFieldName(),
                    mapping.getMcpPath(),
                    mapping.getMcpType(),
                    mapping.getMcpDesc(),
                    mapping.getIsRequired() == null ? 0 : mapping.getIsRequired(),
                    mapping.getSortOrder() == null ? sortOrder++ : mapping.getSortOrder()));
        }
        return result;
    }

    private void validateToolRegisterRequest(ToolRegisterRequest request) {
        if (request == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "请求体不能为空");
        }
        if (isBlank(request.getGatewayId()) || isBlank(request.getToolName()) || isBlank(request.getToolDescription())
                || isBlank(request.getHttpUrl()) || isBlank(request.getHttpMethod())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "gatewayId、toolName、toolDescription、httpUrl、httpMethod 不能为空");
        }
        if (request.getMappings() == null || request.getMappings().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "mappings 至少需要配置一项");
        }
        for (ToolMappingRequest mapping : request.getMappings()) {
            if (mapping == null || isBlank(mapping.getFieldName()) || isBlank(mapping.getMcpPath()) || isBlank(mapping.getMcpType())) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "mapping 的 fieldName、mcpPath、mcpType 不能为空");
            }
        }
    }

    private long nextId() {
        return System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String trimSlash(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class ToolRegisterRequest {
        private String gatewayId;
        private String toolName;
        private String toolType;
        private String toolDescription;
        private String toolVersion;
        private String protocolType;
        private String httpUrl;
        private String httpMethod;
        private String httpHeaders;
        private Integer timeout;
        private Integer retryTimes;
        private Integer status;
        private List<ToolMappingRequest> mappings;

        public String getGatewayId() { return gatewayId; }
        public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public String getToolType() { return toolType; }
        public void setToolType(String toolType) { this.toolType = toolType; }
        public String getToolDescription() { return toolDescription; }
        public void setToolDescription(String toolDescription) { this.toolDescription = toolDescription; }
        public String getToolVersion() { return toolVersion; }
        public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }
        public String getProtocolType() { return protocolType; }
        public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
        public String getHttpUrl() { return httpUrl; }
        public void setHttpUrl(String httpUrl) { this.httpUrl = httpUrl; }
        public String getHttpMethod() { return httpMethod; }
        public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
        public String getHttpHeaders() { return httpHeaders; }
        public void setHttpHeaders(String httpHeaders) { this.httpHeaders = httpHeaders; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public Integer getRetryTimes() { return retryTimes; }
        public void setRetryTimes(Integer retryTimes) { this.retryTimes = retryTimes; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public List<ToolMappingRequest> getMappings() { return mappings; }
        public void setMappings(List<ToolMappingRequest> mappings) { this.mappings = mappings; }
    }

    public static class ToolMappingRequest {
        private String mappingType;
        private String parentPath;
        private String fieldName;
        private String mcpPath;
        private String mcpType;
        private String mcpDesc;
        private Integer isRequired;
        private Integer sortOrder;

        public String getMappingType() { return mappingType; }
        public void setMappingType(String mappingType) { this.mappingType = mappingType; }
        public String getParentPath() { return parentPath; }
        public void setParentPath(String parentPath) { this.parentPath = parentPath; }
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public String getMcpPath() { return mcpPath; }
        public void setMcpPath(String mcpPath) { this.mcpPath = mcpPath; }
        public String getMcpType() { return mcpType; }
        public void setMcpType(String mcpType) { this.mcpType = mcpType; }
        public String getMcpDesc() { return mcpDesc; }
        public void setMcpDesc(String mcpDesc) { this.mcpDesc = mcpDesc; }
        public Integer getIsRequired() { return isRequired; }
        public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

}
