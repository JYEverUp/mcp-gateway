package cn.bugstack.ai.mcpgateway.domain.gateway.service.tool;

import cn.bugstack.ai.mcpgateway.domain.gateway.adapter.repository.IGatewayRepository;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.service.IGatewayToolConfigService;
import org.springframework.stereotype.Service;

@Service
public class GatewayToolConfigService implements IGatewayToolConfigService {

    private final IGatewayRepository repository;

    public GatewayToolConfigService(IGatewayRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity) {
        repository.saveGatewayToolConfig(commandEntity);
    }

    @Override
    public void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity) {
        repository.updateGatewayToolProtocol(commandEntity);
    }
}
