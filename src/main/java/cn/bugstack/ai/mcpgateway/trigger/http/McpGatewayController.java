package cn.bugstack.ai.mcpgateway.trigger.http;

import cn.bugstack.ai.mcpgateway.api.response.Response;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpMessageService;
import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpSessionService;
import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionMessageService;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class McpGatewayController {

    private final IMcpSessionService mcpSessionService;
    private final IMcpMessageService mcpMessageService;
    private final ISessionManagementService sessionManagementService;
    private final ISessionMessageService sessionMessageService;
    private final ObjectMapper objectMapper;

    public McpGatewayController(
            IMcpSessionService mcpSessionService,
            IMcpMessageService mcpMessageService,
            ISessionManagementService sessionManagementService,
            ISessionMessageService sessionMessageService,
            ObjectMapper objectMapper) {
        this.mcpSessionService = mcpSessionService;
        this.mcpMessageService = mcpMessageService;
        this.sessionManagementService = sessionManagementService;
        this.sessionMessageService = sessionMessageService;
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
        if (gatewayId == null || gatewayId.isBlank()) {
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
        if (apiKey != null && !apiKey.isBlank()) {
            messageEndpoint += "&api_key=" + apiKey;
        }
        result.put("messageEndpoint", messageEndpoint);
        result.put("sseEndpoint", "/custom-mcp/sse?gatewayId=" + gatewayId);
        return result;
    }

}
