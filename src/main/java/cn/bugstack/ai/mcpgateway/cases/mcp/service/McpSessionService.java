package cn.bugstack.ai.mcpgateway.cases.mcp.service;

import cn.bugstack.ai.mcpgateway.cases.mcp.IMcpSessionService;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class McpSessionService implements IMcpSessionService {

    private final ISessionManagementService sessionManagementService;

    public McpSessionService(ISessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId, String apiKey) {
        SessionConfigVO session = sessionManagementService.createSession(gatewayId, apiKey);
        return session.getSink().asFlux()
                .doOnCancel(() -> sessionManagementService.removeSession(session.getSessionId()));
    }

}
