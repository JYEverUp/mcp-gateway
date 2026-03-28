package cn.bugstack.ai.mcpgateway.domain.session.model.entity;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;

public class HandleMessageCommandEntity {

    private final String gatewayId;
    private final String apiKey;
    private final String sessionId;
    private final McpSchemaVO.JSONRPCMessage jsonrpcMessage;

    public HandleMessageCommandEntity(String gatewayId, String apiKey, String sessionId, String messageBody) throws Exception {
        this.gatewayId = gatewayId;
        this.apiKey = apiKey;
        this.sessionId = sessionId;
        this.jsonrpcMessage = McpSchemaVO.deserializeJsonRpcMessage(messageBody);
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public McpSchemaVO.JSONRPCMessage getJsonrpcMessage() {
        return jsonrpcMessage;
    }

}
