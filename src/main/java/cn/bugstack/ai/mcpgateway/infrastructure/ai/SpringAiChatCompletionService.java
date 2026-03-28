package cn.bugstack.ai.mcpgateway.infrastructure.ai;

import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpringAiChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatCompletionService.class);

    private final ObjectMapper objectMapper;

    public SpringAiChatCompletionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode chatCompletion(String aiBaseUrl, String aiApiKey, String aiModel, ArrayNode messages, ArrayNode tools) throws Exception {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(trimSlash(aiBaseUrl))
                .apiKey(aiApiKey)
                .completionsPath("/chat/completions")
                .build();

        List<OpenAiApi.ChatCompletionMessage> chatMessages = buildMessages(messages);
        List<OpenAiApi.FunctionTool> functionTools = buildTools(tools);
        OpenAiApi.ChatCompletionRequest request = buildRequest(aiModel, chatMessages, functionTools);

        log.info("springAi chatCompletion invoked, model={}, messageCount={}, toolCount={}",
                aiModel, chatMessages.size(), functionTools.size());

        OpenAiApi.ChatCompletion response = openAiApi.chatCompletionEntity(request).getBody();
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "Spring AI 没有返回有效消息");
        }

        return objectMapper.valueToTree(response);
    }

    public Flux<String> chatCompletionStream(String aiBaseUrl, String aiApiKey, String aiModel, ArrayNode messages, ArrayNode tools) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(trimSlash(aiBaseUrl))
                .apiKey(aiApiKey)
                .completionsPath("/chat/completions")
                .build();

        List<OpenAiApi.ChatCompletionMessage> chatMessages = buildMessages(messages);
        List<OpenAiApi.FunctionTool> functionTools = buildToolsQuietly(tools);
        OpenAiApi.ChatCompletionRequest request = buildStreamRequest(aiModel, chatMessages, functionTools);

        log.info("springAi chatCompletionStream invoked, model={}, messageCount={}, toolCount={}",
                aiModel, chatMessages.size(), functionTools.size());

        return openAiApi.chatCompletionStream(request)
                .map(this::extractChunkText)
                .filter(text -> text != null && !text.isBlank());
    }

    private OpenAiApi.ChatCompletionRequest buildRequest(
            String aiModel,
            List<OpenAiApi.ChatCompletionMessage> chatMessages,
            List<OpenAiApi.FunctionTool> functionTools) {
        if (functionTools == null || functionTools.isEmpty()) {
            return new OpenAiApi.ChatCompletionRequest(chatMessages, aiModel, 0.8D);
        }
        return new OpenAiApi.ChatCompletionRequest(chatMessages, aiModel, functionTools, "auto");
    }

    private OpenAiApi.ChatCompletionRequest buildStreamRequest(
            String aiModel,
            List<OpenAiApi.ChatCompletionMessage> chatMessages,
            List<OpenAiApi.FunctionTool> functionTools) {
        Object toolChoice = functionTools == null || functionTools.isEmpty() ? null : "auto";
        return new OpenAiApi.ChatCompletionRequest(
                chatMessages,
                aiModel,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                0.8D,
                null,
                functionTools == null || functionTools.isEmpty() ? null : functionTools,
                toolChoice,
                null,
                null,
                null,
                null);
    }

    private List<OpenAiApi.ChatCompletionMessage> buildMessages(ArrayNode messages) {
        List<OpenAiApi.ChatCompletionMessage> chatMessages = new ArrayList<>();
        if (messages == null) {
            return chatMessages;
        }

        for (JsonNode messageNode : messages) {
            OpenAiApi.ChatCompletionMessage.Role role = mapRole(messageNode.path("role").asText("user"));
            String content = extractContent(messageNode.path("content"));

            if (role == OpenAiApi.ChatCompletionMessage.Role.TOOL) {
                chatMessages.add(new OpenAiApi.ChatCompletionMessage(
                        content,
                        role,
                        null,
                        textOrNull(messageNode, "tool_call_id"),
                        null,
                        null,
                        null,
                        null));
                continue;
            }

            List<OpenAiApi.ChatCompletionMessage.ToolCall> toolCalls = buildToolCalls(messageNode.path("tool_calls"));
            if (!toolCalls.isEmpty()) {
                chatMessages.add(new OpenAiApi.ChatCompletionMessage(
                        content,
                        role,
                        null,
                        null,
                        toolCalls,
                        null,
                        null,
                        null));
                continue;
            }

            chatMessages.add(new OpenAiApi.ChatCompletionMessage(content, role));
        }
        return chatMessages;
    }

    private List<OpenAiApi.ChatCompletionMessage.ToolCall> buildToolCalls(JsonNode toolCallsNode) {
        List<OpenAiApi.ChatCompletionMessage.ToolCall> toolCalls = new ArrayList<>();
        if (toolCallsNode == null || !toolCallsNode.isArray()) {
            return toolCalls;
        }

        int index = 0;
        for (JsonNode toolCallNode : toolCallsNode) {
            OpenAiApi.ChatCompletionMessage.ChatCompletionFunction function =
                    new OpenAiApi.ChatCompletionMessage.ChatCompletionFunction(
                            toolCallNode.path("function").path("name").asText(),
                            toolCallNode.path("function").path("arguments").asText("{}"));
            toolCalls.add(new OpenAiApi.ChatCompletionMessage.ToolCall(
                    index++,
                    textOrNull(toolCallNode, "id"),
                    toolCallNode.path("type").asText("function"),
                    function));
        }
        return toolCalls;
    }

    private List<OpenAiApi.FunctionTool> buildTools(ArrayNode tools) throws Exception {
        List<OpenAiApi.FunctionTool> functionTools = new ArrayList<>();
        if (tools == null) {
            return functionTools;
        }

        for (JsonNode toolNode : tools) {
            JsonNode functionNode = toolNode.path("function");
            String schema = "{}";
            JsonNode parametersNode = functionNode.path("parameters");
            if (parametersNode != null && !parametersNode.isMissingNode() && !parametersNode.isNull()) {
                schema = objectMapper.writeValueAsString(objectMapper.convertValue(parametersNode, Map.class));
            }

            OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(
                    textOrNull(functionNode, "description"),
                    functionNode.path("name").asText(),
                    schema);
            functionTools.add(new OpenAiApi.FunctionTool(function));
        }

        return functionTools;
    }

    private List<OpenAiApi.FunctionTool> buildToolsQuietly(ArrayNode tools) {
        try {
            return buildTools(tools);
        } catch (Exception e) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "构建 AI 工具定义失败: " + e.getMessage());
        }
    }

    private String extractChunkText(OpenAiApi.ChatCompletionChunk chunk) {
        if (chunk == null || chunk.choices() == null || chunk.choices().isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (OpenAiApi.ChatCompletionChunk.ChunkChoice choice : chunk.choices()) {
            if (choice == null || choice.delta() == null) {
                continue;
            }
            String content = extractMessageContent(choice.delta().content());
            if (content != null && !content.isBlank()) {
                builder.append(content);
            }
        }
        return builder.toString();
    }

    private OpenAiApi.ChatCompletionMessage.Role mapRole(String role) {
        return switch (role.toLowerCase()) {
            case "system" -> OpenAiApi.ChatCompletionMessage.Role.SYSTEM;
            case "assistant" -> OpenAiApi.ChatCompletionMessage.Role.ASSISTANT;
            case "tool" -> OpenAiApi.ChatCompletionMessage.Role.TOOL;
            default -> OpenAiApi.ChatCompletionMessage.Role.USER;
        };
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
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
            String merged = builder.toString().trim();
            return merged.isEmpty() ? objectToJson(contentNode) : merged;
        }
        return objectToJson(contentNode);
    }

    private String extractMessageContent(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof String text) {
            return text;
        }
        JsonNode contentNode = objectMapper.valueToTree(content);
        return extractContent(contentNode);
    }

    private String objectToJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

}
