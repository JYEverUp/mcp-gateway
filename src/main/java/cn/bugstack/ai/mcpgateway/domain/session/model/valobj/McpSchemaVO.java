package cn.bugstack.ai.mcpgateway.domain.session.model.valobj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class McpSchemaVO {

    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";
    public static final String JSONRPC_VERSION = "2.0";

    private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpSchemaVO() {
    }

    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) throws IOException {
        Map<String, Object> map = OBJECT_MAPPER.readValue(jsonText, MAP_TYPE_REF);
        if (map.containsKey("method") && map.containsKey("id")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCRequest.class);
        }
        if (map.containsKey("method")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCNotification.class);
        }
        if (map.containsKey("result") || map.containsKey("error")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCResponse.class);
        }
        throw new IllegalArgumentException("无法识别的 JSON-RPC 消息");
    }

    public static <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return OBJECT_MAPPER.convertValue(data, typeRef);
    }

    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCNotification, JSONRPCResponse {
        String jsonrpc();
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest(
            @JsonProperty("jsonrpc") String jsonrpc,
            @JsonProperty("method") String method,
            @JsonProperty("id") Object id,
            @JsonProperty("params") Object params) implements JSONRPCMessage {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCNotification(
            @JsonProperty("jsonrpc") String jsonrpc,
            @JsonProperty("method") String method,
            @JsonProperty("params") Object params) implements JSONRPCMessage {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCResponse(
            @JsonProperty("jsonrpc") String jsonrpc,
            @JsonProperty("id") Object id,
            @JsonProperty("result") Object result,
            @JsonProperty("error") JSONRPCError error) implements JSONRPCMessage {

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record JSONRPCError(
                @JsonProperty("code") int code,
                @JsonProperty("message") String message,
                @JsonProperty("data") Object data) {
        }
    }

    public sealed interface Request permits InitializeRequest, CallToolRequest {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeRequest(
            @JsonProperty("protocolVersion") String protocolVersion,
            @JsonProperty("capabilities") ClientCapabilities capabilities,
            @JsonProperty("clientInfo") Implementation clientInfo) implements Request {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeResult(
            @JsonProperty("protocolVersion") String protocolVersion,
            @JsonProperty("capabilities") ServerCapabilities capabilities,
            @JsonProperty("serverInfo") Implementation serverInfo,
            @JsonProperty("instructions") String instructions) {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClientCapabilities(
            @JsonProperty("experimental") Map<String, Object> experimental,
            @JsonProperty("roots") RootCapabilities roots,
            @JsonProperty("sampling") Sampling sampling) {

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record RootCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record Sampling() {
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerCapabilities(
            @JsonProperty("completions") CompletionCapabilities completions,
            @JsonProperty("experimental") Map<String, Object> experimental,
            @JsonProperty("logging") LoggingCapabilities logging,
            @JsonProperty("prompts") PromptCapabilities prompts,
            @JsonProperty("resources") ResourceCapabilities resources,
            @JsonProperty("tools") ToolCapabilities tools) {

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record CompletionCapabilities() {
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record LoggingCapabilities() {
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record PromptCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record ResourceCapabilities(
                @JsonProperty("subscribe") Boolean subscribe,
                @JsonProperty("listChanged") Boolean listChanged) {
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record ToolCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Implementation(
            @JsonProperty("name") String name,
            @JsonProperty("version") String version) {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("inputSchema") JsonSchema inputSchema) {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema(
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required,
            @JsonProperty("additionalProperties") Boolean additionalProperties,
            @JsonProperty("$defs") Map<String, Object> defs,
            @JsonProperty("definitions") Map<String, Object> definitions) {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolRequest(
            @JsonProperty("name") String name,
            @JsonProperty("arguments") Map<String, Object> arguments) implements Request {
    }

}
