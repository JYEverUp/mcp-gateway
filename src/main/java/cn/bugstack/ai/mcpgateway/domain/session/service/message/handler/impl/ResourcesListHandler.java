package cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.impl;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.IRequestHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("resourcesListHandler")
public class ResourcesListHandler implements IRequestHandler {

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        return new McpSchemaVO.JSONRPCResponse(
                McpSchemaVO.JSONRPC_VERSION,
                message.id(),
                Map.of("resources", List.of()),
                null);
    }

}
