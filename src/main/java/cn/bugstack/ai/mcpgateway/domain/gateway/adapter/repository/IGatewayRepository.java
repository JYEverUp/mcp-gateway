package cn.bugstack.ai.mcpgateway.domain.gateway.adapter.repository;

import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayToolConfigCommandEntity;

public interface IGatewayRepository {

    void saveGatewayConfig(GatewayConfigCommandEntity commandEntity);

    void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity);

    void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity);

    void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity);

}
