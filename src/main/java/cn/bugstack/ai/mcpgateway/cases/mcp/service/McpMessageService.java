package cn.bugstack.ai.mcpgateway.cases.mcp.service;

import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpMessageService;
import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionMessageService;
import cn.bugstack.ai.mcpgateway.types.enums.McpErrorCodes;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

@Service
public class McpMessageService implements IMcpMessageService {

    private final ISessionManagementService sessionManagementService;
    private final ISessionMessageService sessionMessageService;
    private final ObjectMapper objectMapper;

    public McpMessageService(
            ISessionManagementService sessionManagementService,
            ISessionMessageService sessionMessageService,
            ObjectMapper objectMapper) {
        this.sessionManagementService = sessionManagementService;
        this.sessionMessageService = sessionMessageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<Void> handleMessage(HandleMessageCommandEntity commandEntity) throws Exception {
        SessionConfigVO session = sessionManagementService.getSession(commandEntity.getSessionId());
        if (session == null) {
            throw new AppException(String.valueOf(McpErrorCodes.SESSION_NOT_FOUND), "会话不存在或已过期");
        }

        McpSchemaVO.JSONRPCResponse response = sessionMessageService.processHandlerMessage(commandEntity);
        if (response == null) {
            return ResponseEntity.accepted().build();
        }

        session.getSink().tryEmitNext(ServerSentEvent.<String>builder()
                .event("message")
                .data(objectMapper.writeValueAsString(response))
                .build());
        return ResponseEntity.accepted().build();
    }

}
