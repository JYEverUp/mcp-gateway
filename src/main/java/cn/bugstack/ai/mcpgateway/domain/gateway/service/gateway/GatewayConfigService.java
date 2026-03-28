package cn.bugstack.ai.mcpgateway.domain.gateway.service.gateway;

import cn.bugstack.ai.mcpgateway.domain.gateway.adapter.repository.IGatewayRepository;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.service.IGatewayConfigService;
import org.springframework.stereotype.Service;

@Service
public class GatewayConfigService implements IGatewayConfigService {

    private final IGatewayRepository repository;

    public GatewayConfigService(IGatewayRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveGatewayConfig(GatewayConfigCommandEntity commandEntity) {
        repository.saveGatewayConfig(commandEntity);
    }

    @Override
    public void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity) {
        repository.updateGatewayAuthStatus(commandEntity);
    }
}
