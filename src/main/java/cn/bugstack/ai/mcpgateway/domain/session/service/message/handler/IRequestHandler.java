package cn.bugstack.ai.mcpgateway.domain.session.service.message.handler;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;

public interface IRequestHandler {

    McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message);

}
