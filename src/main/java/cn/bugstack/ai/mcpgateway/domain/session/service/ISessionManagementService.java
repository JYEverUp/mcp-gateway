package cn.bugstack.ai.mcpgateway.domain.session.service;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;

public interface ISessionManagementService {

    SessionConfigVO createSession(String gatewayId, String apiKey);

    void removeSession(String sessionId);

    SessionConfigVO getSession(String sessionId);

    void shutdown();

}
