package cn.bugstack.ai.mcpgateway.domain.gateway.service;

import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayConfigCommandEntity;

public interface IGatewayConfigService {

    void saveGatewayConfig(GatewayConfigCommandEntity commandEntity);

    void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity);

}
