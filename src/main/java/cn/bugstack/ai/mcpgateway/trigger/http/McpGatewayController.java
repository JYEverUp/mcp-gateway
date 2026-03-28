package cn.bugstack.ai.mcpgateway.trigger.http;

import cn.bugstack.ai.mcpgateway.api.IMcpGatewayService;
import cn.bugstack.ai.mcpgateway.api.response.Response;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpMessageService;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpSessionService;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.gateway.service.IGatewayConfigService;
import cn.bugstack.ai.mcpgateway.domain.gateway.service.IGatewayToolConfigService;
import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionMessageService;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpAiModelConfigDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpAiModelConfigPO;
import cn.bugstack.ai.mcpgateway.infrastructure.ai.SpringAiChatCompletionService;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.enums.GatewayEnum;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class McpGatewayController implements IMcpGatewayService {

    private static final Logger log = LoggerFactory.getLogger(McpGatewayController.class);

    private final IMcpSessionService mcpSessionService;
    private final IMcpMessageService mcpMessageService;
    private final ISessionManagementService sessionManagementService;
    private final ISessionMessageService sessionMessageService;
    private final ISessionRepository sessionRepository;
    private final IGatewayConfigService gatewayConfigService;
    private final IGatewayToolConfigService gatewayToolConfigService;
    private final IMcpGatewayDao mcpGatewayDao;
    private final IMcpAiModelConfigDao mcpAiModelConfigDao;
    private final SpringAiChatCompletionService springAiChatCompletionService;
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
            IGatewayConfigService gatewayConfigService,
            IGatewayToolConfigService gatewayToolConfigService,
            IMcpGatewayDao mcpGatewayDao,
            IMcpAiModelConfigDao mcpAiModelConfigDao,
            SpringAiChatCompletionService springAiChatCompletionService,
            ObjectMapper objectMapper) {
        this.mcpSessionService = mcpSessionService;
        this.mcpMessageService = mcpMessageService;
        this.sessionManagementService = sessionManagementService;
        this.sessionMessageService = sessionMessageService;
        this.sessionRepository = sessionRepository;
        this.gatewayConfigService = gatewayConfigService;
        this.gatewayToolConfigService = gatewayToolConfigService;
        this.mcpGatewayDao = mcpGatewayDao;
        this.mcpAiModelConfigDao = mcpAiModelConfigDao;
        this.springAiChatCompletionService = springAiChatCompletionService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "{gatewayId}/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleSseConnection(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        log.info("handleSseConnection invoked, gatewayId={}, apiKeyPresent={}", gatewayId, !isBlank(apiKey));
        validateGatewayId(gatewayId);
        return mcpSessionService.createMcpSession(gatewayId, apiKey);
    }

    @GetMapping(value = "custom-mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleCustomSseConnection(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        log.info("handleCustomSseConnection invoked, gatewayId={}, apiKeyPresent={}", gatewayId, !isBlank(apiKey));
        return handleSseConnection(gatewayId, apiKey);
    }

    @PostMapping(value = "{gatewayId}/mcp/sse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleMessage(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        try {
            log.info("handleMessage invoked, gatewayId={}, sessionId={}, apiKeyPresent={}, body={}",
                    gatewayId, sessionId, !isBlank(apiKey), messageBody);
            validateGatewayId(gatewayId);
            if (sessionId == null || sessionId.isBlank()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "sessionId 不能为空");
            }

            HandleMessageCommandEntity commandEntity = new HandleMessageCommandEntity(gatewayId, apiKey, sessionId, messageBody);
            return Mono.just(mcpMessageService.handleMessage(commandEntity));
        } catch (Exception e) {
            log.error("handleMessage failed, gatewayId={}, sessionId={}", gatewayId, sessionId, e);
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    @PostMapping(value = "custom-mcp/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleCustomMessage(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        log.info("handleCustomMessage invoked, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return handleMessage(gatewayId, sessionId, apiKey, messageBody);
    }

    @PostMapping(value = "custom-mcp/message/result", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleCustomMessageWithResult(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        log.info("handleCustomMessageWithResult invoked, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return debugHandleMessage(gatewayId, sessionId, apiKey, messageBody);
    }

    @PostMapping(value = "custom-mcp/gateway/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerGateway(@RequestBody GatewayRegisterRequest request) {
        try {
            log.info("registerGateway invoked, request={}", writeValue(request));
            validateGatewayRegisterRequest(request);
            GatewayConfigCommandEntity commandEntity = new GatewayConfigCommandEntity();
            commandEntity.setGatewayConfigVO(new GatewayConfigVO(
                    request.getGatewayId(),
                    request.getGatewayName(),
                    request.getGatewayDesc(),
                    isBlank(request.getVersion()) ? "1.0.0" : request.getVersion(),
                    request.getAuth() == null ? GatewayEnum.GatewayAuthStatusEnum.ENABLE : GatewayEnum.GatewayAuthStatusEnum.getByCode(request.getAuth()),
                    request.getStatus() == null ? GatewayEnum.GatewayStatus.NOT_VERIFIED : GatewayEnum.GatewayStatus.get(request.getStatus())));
            gatewayConfigService.saveGatewayConfig(commandEntity);
            Map<String, Object> result = Map.of(
                    "gatewayId", request.getGatewayId(),
                    "gatewayName", request.getGatewayName(),
                    "status", "CREATED");
            log.info("registerGateway completed, result={}", result);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("registerGateway validation failed, request={}", writeValue(request), e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("registerGateway failed, request={}", writeValue(request), e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/gateway/auth/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateGatewayAuth(@RequestBody GatewayAuthUpdateRequest request) {
        try {
            log.info("updateGatewayAuth invoked, request={}", writeValue(request));
            if (request == null || isBlank(request.getGatewayId()) || request.getAuth() == null) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "gatewayId、auth 不能为空");
            }
            gatewayConfigService.updateGatewayAuthStatus(
                    GatewayConfigCommandEntity.buildUpdateGatewayAuthStatusVO(
                            request.getGatewayId(),
                            GatewayEnum.GatewayAuthStatusEnum.getByCode(request.getAuth())));
            Map<String, Object> result = Map.of(
                    "gatewayId", request.getGatewayId(),
                    "auth", request.getAuth(),
                    "updated", true);
            log.info("updateGatewayAuth completed, result={}", result);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("updateGatewayAuth validation failed, request={}", writeValue(request), e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("updateGatewayAuth failed, request={}", writeValue(request), e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "custom-mcp/gateway/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryGatewayDetail(@RequestParam("gatewayId") String gatewayId) {
        try {
            log.info("queryGatewayDetail invoked, gatewayId={}", gatewayId);
            validateGatewayId(gatewayId);
            var gateway = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
            if (gateway == null) {
                log.info("queryGatewayDetail miss, gatewayId={}", gatewayId);
                return ResponseEntity.notFound().build();
            }
            log.info("queryGatewayDetail completed, gatewayId={}, result={}", gatewayId, gateway);
            return ResponseEntity.ok(gateway);
        } catch (AppException e) {
            log.warn("queryGatewayDetail validation failed, gatewayId={}", gatewayId, e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("queryGatewayDetail failed, gatewayId={}", gatewayId, e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "custom-mcp/gateway/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryGatewayList() {
        try {
            log.info("queryGatewayList invoked");
            List<?> result = mcpGatewayDao.queryAll();
            log.info("queryGatewayList completed, count={}", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("queryGatewayList failed", e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "custom-mcp/ai/model/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryAiModelConfigList() {
        try {
            log.info("queryAiModelConfigList invoked");
            List<McpAiModelConfigPO> configs = mcpAiModelConfigDao.queryAllActive();
            List<Map<String, Object>> result = new ArrayList<>();
            for (McpAiModelConfigPO config : configs) {
                result.add(buildAiModelConfigView(config));
            }
            log.info("queryAiModelConfigList completed, count={}", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("queryAiModelConfigList failed", e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/chat-with-tools", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatWithTools(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("chatWithTools invoked, gatewayId={}, sessionId={}, apiKeyPresent={}, request={}",
                    gatewayId, sessionId, !isBlank(apiKey), requestBody);
            validateGatewayId(gatewayId);
            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "sessionId 不能为空"));
            }

            SessionConfigVO session = sessionManagementService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            AiChatConfig aiChatConfig = resolveAiChatConfig(requestBody);
            String userQuestion = stringValue(requestBody.get("userQuestion"));
            String systemPrompt = stringValue(requestBody.get("systemPrompt"));
            boolean enableMcpTools = booleanValue(requestBody.get("enableMcpTools"), true);

            if (isBlank(userQuestion)) {
                return ResponseEntity.badRequest().body(Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "userQuestion 不能为空"));
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

            JsonNode firstAiResponse = springAiChatCompletionService.chatCompletion(
                    aiChatConfig.aiBaseUrl(),
                    aiChatConfig.aiApiKey(),
                    aiChatConfig.aiModel(),
                    messages,
                    openAiTools.isEmpty() ? null : openAiTools);
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

            JsonNode finalAiResponse = toolCalls.isEmpty()
                    ? firstAiResponse
                    : springAiChatCompletionService.chatCompletion(aiChatConfig.aiBaseUrl(), aiChatConfig.aiApiKey(), aiChatConfig.aiModel(), messages, null);
            JsonNode finalMessage = finalAiResponse.path("choices").path(0).path("message");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gatewayId", gatewayId);
            result.put("sessionId", sessionId);
            result.put("usedMcpTools", enableMcpTools);
            result.put("aiConfigId", aiChatConfig.aiConfigId());
            result.put("aiModel", aiChatConfig.aiModel());
            result.put("toolCount", mcpTools.size());
            result.put("toolExecutions", toolExecutions);
            result.put("answer", extractMessageText(finalMessage));
            result.put("firstModelResponse", firstAiResponse);
            if (!toolCalls.isEmpty()) {
                result.put("finalModelResponse", finalAiResponse);
            }
            log.info("chatWithTools completed, gatewayId={}, sessionId={}, toolExecutions={}, answerLength={}",
                    gatewayId, sessionId, toolExecutions.size(), finalMessage == null ? 0 : extractMessageText(finalMessage).length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("chatWithTools failed, gatewayId={}, sessionId={}", gatewayId, sessionId, e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/chat-with-tools/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithToolsStream(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("chatWithToolsStream invoked, gatewayId={}, sessionId={}, apiKeyPresent={}, request={}",
                    gatewayId, sessionId, !isBlank(apiKey), requestBody);
            validateGatewayId(gatewayId);
            if (isBlank(sessionId)) {
                return Flux.just(sseEvent("error", "sessionId 不能为空"));
            }

            SessionConfigVO session = sessionManagementService.getSession(sessionId);
            if (session == null) {
                return Flux.just(sseEvent("error", "会话不存在，请先建立连接"));
            }

            AiChatConfig aiChatConfig = resolveAiChatConfig(requestBody);
            String userQuestion = stringValue(requestBody.get("userQuestion"));
            String systemPrompt = stringValue(requestBody.get("systemPrompt"));
            boolean enableMcpTools = booleanValue(requestBody.get("enableMcpTools"), true);

            if (isBlank(userQuestion)) {
                return Flux.just(sseEvent("error", "userQuestion 不能为空"));
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

            JsonNode firstAiResponse = springAiChatCompletionService.chatCompletion(
                    aiChatConfig.aiBaseUrl(),
                    aiChatConfig.aiApiKey(),
                    aiChatConfig.aiModel(),
                    messages,
                    openAiTools.isEmpty() ? null : openAiTools);
            JsonNode firstMessage = firstAiResponse.path("choices").path(0).path("message");
            if (firstMessage.isMissingNode() || firstMessage.isNull()) {
                return Flux.just(sseEvent("error", "AI 没有返回有效消息"));
            }

            messages.add(firstMessage);
            ArrayNode toolCalls = arrayNodeOf(firstMessage.path("tool_calls"));
            List<Map<String, Object>> toolExecutions = new ArrayList<>();
            List<ServerSentEvent<String>> warmupEvents = new ArrayList<>();
            warmupEvents.add(sseEvent("status", "AI 请求已受理"));

            for (JsonNode toolCall : toolCalls) {
                String toolName = toolCall.path("function").path("name").asText();
                JsonNode argumentsNode = parseToolArguments(toolCall.path("function").path("arguments").asText("{}"));
                warmupEvents.add(sseEvent("status", "正在调用工具: " + toolName));

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

            Flux<ServerSentEvent<String>> answerFlux;
            if (toolCalls.isEmpty()) {
                String answer = extractMessageText(firstMessage);
                answerFlux = Flux.just(sseEvent("answer", answer));
            } else {
                warmupEvents.add(sseEvent("status", "工具调用完成，开始生成最终答案"));
                answerFlux = springAiChatCompletionService.chatCompletionStream(aiChatConfig.aiBaseUrl(), aiChatConfig.aiApiKey(), aiChatConfig.aiModel(), messages, null)
                        .map(chunk -> sseEvent("answer", chunk));
            }

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("gatewayId", gatewayId);
            meta.put("sessionId", sessionId);
            meta.put("usedMcpTools", enableMcpTools);
            meta.put("aiConfigId", aiChatConfig.aiConfigId());
            meta.put("aiModel", aiChatConfig.aiModel());
            meta.put("toolCount", mcpTools.size());
            meta.put("toolExecutions", toolExecutions);

            return Flux.concat(
                    Flux.fromIterable(warmupEvents),
                    answerFlux,
                    Flux.just(sseEvent("done", writeValue(meta))));
        } catch (Exception e) {
            log.error("chatWithToolsStream failed, gatewayId={}, sessionId={}", gatewayId, sessionId, e);
            return Flux.just(sseEvent("error", e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/tool/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerTool(@RequestBody ToolRegisterRequest request) {
        try {
            log.info("registerTool invoked, request={}", writeValue(request));
            validateToolRegisterRequest(request);
            Map<String, Object> httpProbe = probeHttpEndpoint(request.getHttpUrl());
            if (!(Boolean) httpProbe.get("reachable")) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "HTTP 地址不可达，不能新增工具");
            }

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
            log.info("registerTool completed, result={}", result);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("registerTool validation failed, request={}", writeValue(request), e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("registerTool failed, request={}", writeValue(request), e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/tool/http/check", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkToolHttp(@RequestBody ToolHttpCheckRequest request) {
        try {
            log.info("checkToolHttp invoked, request={}", writeValue(request));
            if (request == null || isBlank(request.getHttpUrl())) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "httpUrl 不能为空");
            }
            Map<String, Object> result = probeHttpEndpoint(request.getHttpUrl());
            log.info("checkToolHttp completed, result={}", result);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("checkToolHttp validation failed, request={}", writeValue(request), e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("checkToolHttp failed, request={}", writeValue(request), e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @PostMapping(value = "custom-mcp/tool/protocol/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateToolProtocol(@RequestBody ToolProtocolUpdateRequest request) {
        try {
            log.info("updateToolProtocol invoked, request={}", writeValue(request));
            if (request == null || isBlank(request.getGatewayId()) || request.getProtocolId() == null || isBlank(request.getProtocolType())) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "gatewayId、protocolId、protocolType 不能为空");
            }
            GatewayToolConfigCommandEntity commandEntity = new GatewayToolConfigCommandEntity();
            commandEntity.setGatewayToolConfigVO(new GatewayToolConfigVO(
                    request.getGatewayId(),
                    request.getToolId(),
                    null,
                    null,
                    null,
                    null,
                    request.getProtocolId(),
                    request.getProtocolType()));
            gatewayToolConfigService.updateGatewayToolProtocol(commandEntity);
            Map<String, Object> result = Map.of(
                    "gatewayId", request.getGatewayId(),
                    "toolId", request.getToolId(),
                    "protocolId", request.getProtocolId(),
                    "protocolType", request.getProtocolType(),
                    "updated", true);
            log.info("updateToolProtocol completed, result={}", result);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("updateToolProtocol validation failed, request={}", writeValue(request), e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("updateToolProtocol failed, request={}", writeValue(request), e);
            return ResponseEntity.internalServerError().body(Response.error(ResponseCode.UN_ERROR.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "custom-mcp/tool/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryToolList(@RequestParam("gatewayId") String gatewayId) {
        try {
            log.info("queryToolList invoked, gatewayId={}", gatewayId);
            validateGatewayId(gatewayId);
            List<Map<String, Object>> result = sessionRepository.queryToolDetailsByGatewayId(gatewayId);
            log.info("queryToolList completed, gatewayId={}, count={}", gatewayId, result.size());
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            log.warn("queryToolList validation failed, gatewayId={}", gatewayId, e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("queryToolList failed, gatewayId={}", gatewayId, e);
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
        log.info("debug health invoked, result={}", result);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "debug/mcp/{gatewayId}/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            @PathVariable("gatewayId") String gatewayId,
            @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        log.info("getSessionInfo invoked, gatewayId={}, sessionId={}, apiKeyPresent={}", gatewayId, sessionId, !isBlank(apiKey));
        SessionConfigVO session = sessionManagementService.getSession(sessionId);
        if (session == null) {
            log.info("getSessionInfo miss, gatewayId={}, sessionId={}", gatewayId, sessionId);
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = buildSessionPayload(gatewayId, session, apiKey);
        log.info("getSessionInfo completed, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "custom-mcp/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCustomSessionInfo(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey) {
        log.info("getCustomSessionInfo invoked, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return getSessionInfo(gatewayId, sessionId, apiKey);
    }

    @DeleteMapping(value = "debug/mcp/{gatewayId}/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> disconnectSession(
            @PathVariable("gatewayId") String gatewayId,
            @PathVariable("sessionId") String sessionId) {
        log.info("disconnectSession invoked, gatewayId={}, sessionId={}", gatewayId, sessionId);
        SessionConfigVO session = sessionManagementService.getSession(sessionId);
        if (session == null) {
            log.info("disconnectSession miss, gatewayId={}, sessionId={}", gatewayId, sessionId);
            return ResponseEntity.notFound().build();
        }

        sessionManagementService.removeSession(sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("sessionId", sessionId);
        result.put("disconnected", true);
        result.put("timestamp", Instant.now().toString());
        log.info("disconnectSession completed, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping(value = "custom-mcp/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> disconnectCustomSession(
            @RequestParam("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId) {
        log.info("disconnectCustomSession invoked, gatewayId={}, sessionId={}", gatewayId, sessionId);
        return disconnectSession(gatewayId, sessionId);
    }

    @PostMapping(value = "debug/mcp/{gatewayId}/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> debugHandleMessage(
            @PathVariable("gatewayId") String gatewayId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody) {
        try {
            log.info("debugHandleMessage invoked, gatewayId={}, sessionId={}, apiKeyPresent={}, body={}",
                    gatewayId, sessionId, !isBlank(apiKey), messageBody);
            SessionConfigVO session = sessionManagementService.getSession(sessionId);
            if (session == null) {
                log.info("debugHandleMessage miss, gatewayId={}, sessionId={}", gatewayId, sessionId);
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
            log.info("debugHandleMessage completed, gatewayId={}, sessionId={}, hasResponse={}",
                    gatewayId, sessionId, response != null);
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            log.warn("debugHandleMessage validation failed, gatewayId={}, sessionId={}", gatewayId, sessionId, e);
            return ResponseEntity.badRequest().body(Response.error(e.getCode(), e.getInfo()));
        } catch (Exception e) {
            log.error("debugHandleMessage failed, gatewayId={}, sessionId={}", gatewayId, sessionId, e);
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

    private AiChatConfig resolveAiChatConfig(Map<String, Object> requestBody) {
        String aiConfigId = stringValue(requestBody.get("aiConfigId"));
        McpAiModelConfigPO config = isBlank(aiConfigId)
                ? mcpAiModelConfigDao.queryDefaultConfig()
                : mcpAiModelConfigDao.queryByConfigId(aiConfigId);

        if (config != null && config.getStatus() != null && config.getStatus() == 1) {
            if (isBlank(config.getBaseUrl()) || isBlank(config.getApiKey()) || isBlank(config.getModelName())) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "数据库里的 AI 模型配置不完整");
            }
            return new AiChatConfig(config.getConfigId(), config.getBaseUrl(), config.getApiKey(), config.getModelName());
        }

        String aiBaseUrl = stringValue(requestBody.get("aiBaseUrl"));
        String aiApiKey = stringValue(requestBody.get("aiApiKey"));
        String aiModel = stringValue(requestBody.get("aiModel"));
        if (isBlank(aiBaseUrl) || isBlank(aiApiKey) || isBlank(aiModel)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "请选择有效的 AI 模型配置");
        }
        return new AiChatConfig(isBlank(aiConfigId) ? "manual" : aiConfigId, aiBaseUrl, aiApiKey, aiModel);
    }

    private Map<String, Object> buildAiModelConfigView(McpAiModelConfigPO config) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("configId", config.getConfigId());
        item.put("provider", config.getProvider());
        item.put("modelName", config.getModelName());
        item.put("displayName", config.getDisplayName());
        item.put("baseUrl", config.getBaseUrl());
        item.put("isDefault", config.getIsDefault());
        item.put("sortOrder", config.getSortOrder());
        item.put("remark", config.getRemark());
        return item;
    }

    private Map<String, Object> probeHttpEndpoint(String httpUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("httpUrl", httpUrl);
        result.put("reachable", false);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(httpUrl))
                    .timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            boolean reachable = statusCode != 404 && statusCode != 410;
            result.put("reachable", reachable);
            result.put("statusCode", statusCode);
            result.put("checkedMethod", "HEAD");
            return result;
        } catch (Exception e) {
            log.warn("probeHttpEndpoint failed, httpUrl={}", httpUrl, e);
            result.put("message", e.getMessage());
            return result;
        }
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

    private ArrayNode arrayNodeOf(JsonNode node) {
        if (node instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        return objectMapper.createArrayNode();
    }

    private ServerSentEvent<String> sseEvent(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data == null ? "" : data)
                .build();
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

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private record AiChatConfig(String aiConfigId, String aiBaseUrl, String aiApiKey, String aiModel) {
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

    public static class ToolHttpCheckRequest {
        private String httpUrl;

        public String getHttpUrl() { return httpUrl; }
        public void setHttpUrl(String httpUrl) { this.httpUrl = httpUrl; }
    }

    public static class GatewayRegisterRequest {
        private String gatewayId;
        private String gatewayName;
        private String gatewayDesc;
        private String version;
        private Integer auth;
        private Integer status;

        public String getGatewayId() { return gatewayId; }
        public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
        public String getGatewayName() { return gatewayName; }
        public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }
        public String getGatewayDesc() { return gatewayDesc; }
        public void setGatewayDesc(String gatewayDesc) { this.gatewayDesc = gatewayDesc; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public Integer getAuth() { return auth; }
        public void setAuth(Integer auth) { this.auth = auth; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class GatewayAuthUpdateRequest {
        private String gatewayId;
        private Integer auth;

        public String getGatewayId() { return gatewayId; }
        public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
        public Integer getAuth() { return auth; }
        public void setAuth(Integer auth) { this.auth = auth; }
    }

    public static class ToolProtocolUpdateRequest {
        private String gatewayId;
        private Long toolId;
        private Long protocolId;
        private String protocolType;

        public String getGatewayId() { return gatewayId; }
        public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
        public Long getToolId() { return toolId; }
        public void setToolId(Long toolId) { this.toolId = toolId; }
        public Long getProtocolId() { return protocolId; }
        public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }
        public String getProtocolType() { return protocolType; }
        public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
    }

    private void validateGatewayRegisterRequest(GatewayRegisterRequest request) {
        if (request == null || isBlank(request.getGatewayId()) || isBlank(request.getGatewayName())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "gatewayId、gatewayName 不能为空");
        }
    }

}
