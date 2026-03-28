package cn.bugstack.ai.mcpgateway.domain.session.service.message;

import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionMessageService;
import cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.IRequestHandler;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SessionMessageService implements ISessionMessageService {

    private final Map<String, IRequestHandler> requestHandlerMap;

    public SessionMessageService(Map<String, IRequestHandler> requestHandlerMap) {
        this.requestHandlerMap = requestHandlerMap;
    }

    @Override
    public McpSchemaVO.JSONRPCResponse processHandlerMessage(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        if (message instanceof McpSchemaVO.JSONRPCRequest request) {
            SessionMessageHandlerMethodEnum methodEnum = SessionMessageHandlerMethodEnum.getByMethod(request.method());
            if (methodEnum == null) {
                throw new AppException(ResponseCode.METHOD_NOT_FOUND.getCode(), ResponseCode.METHOD_NOT_FOUND.getInfo());
            }

            IRequestHandler handler = requestHandlerMap.get(methodEnum.getHandlerName());
            if (handler == null) {
                throw new AppException(ResponseCode.METHOD_NOT_FOUND.getCode(), ResponseCode.METHOD_NOT_FOUND.getInfo());
            }
            return handler.handle(gatewayId, request);
        }
        return null;
    }

    @Override
    public McpSchemaVO.JSONRPCResponse processHandlerMessage(HandleMessageCommandEntity commandEntity) {
        return processHandlerMessage(commandEntity.getGatewayId(), commandEntity.getJsonrpcMessage());
    }

}
