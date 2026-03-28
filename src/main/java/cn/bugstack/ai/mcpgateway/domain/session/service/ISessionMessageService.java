package cn.bugstack.ai.mcpgateway.domain.session.service;

import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;

public interface ISessionMessageService {

    McpSchemaVO.JSONRPCResponse processHandlerMessage(String gatewayId, McpSchemaVO.JSONRPCMessage message);

    McpSchemaVO.JSONRPCResponse processHandlerMessage(HandleMessageCommandEntity commandEntity);

}
