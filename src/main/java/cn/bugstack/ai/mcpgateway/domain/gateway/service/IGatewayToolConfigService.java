package cn.bugstack.ai.mcpgateway.domain.gateway.service;

import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayToolConfigCommandEntity;

public interface IGatewayToolConfigService {

    void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity);

    void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity);

}
