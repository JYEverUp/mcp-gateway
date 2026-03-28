package cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.impl;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.port.ISessionPort;
import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.IRequestHandler;
import cn.bugstack.ai.mcpgateway.types.enums.McpErrorCodes;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("toolsCallHandler")
public class ToolsCallHandler implements IRequestHandler {

    private final ISessionRepository repository;
    private final ISessionPort port;

    public ToolsCallHandler(ISessionRepository repository, ISessionPort port) {
        this.repository = repository;
        this.port = port;
    }

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        try {
            McpSchemaVO.CallToolRequest request = McpSchemaVO.unmarshalFrom(message.params(), new TypeReference<>() {
            });
            McpToolProtocolConfigVO protocolConfig = repository.queryMcpGatewayProtocolConfig(gatewayId, request.name());
            if (protocolConfig == null) {
                throw new AppException(ResponseCode.METHOD_NOT_FOUND.getCode(), "未找到工具协议配置");
            }

            Object result = port.toolCall(protocolConfig.getHttpConfig(), request.arguments());
            return new McpSchemaVO.JSONRPCResponse(
                    McpSchemaVO.JSONRPC_VERSION,
                    message.id(),
                    Map.of(
                            "content", new Object[]{Map.of("type", "text", "text", String.valueOf(result))},
                            "isError", false),
                    null);
        } catch (Exception e) {
            return new McpSchemaVO.JSONRPCResponse(
                    McpSchemaVO.JSONRPC_VERSION,
                    message.id(),
                    null,
                    new McpSchemaVO.JSONRPCResponse.JSONRPCError(McpErrorCodes.INVALID_PARAMS, e.getMessage(), null));
        }
    }

}
