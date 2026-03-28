package cn.bugstack.ai.mcpgateway.infrastructure.adapter.repository;

import cn.bugstack.ai.mcpgateway.domain.gateway.adapter.repository.IGatewayRepository;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayToolConfigVO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayToolDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayPO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayToolPO;
import cn.bugstack.ai.mcpgateway.types.enums.GatewayEnum;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import cn.bugstack.ai.mcpgateway.types.exception.AppException;
import org.springframework.stereotype.Repository;

@Repository
public class GatewayRepository implements IGatewayRepository {

    private final IMcpGatewayDao mcpGatewayDao;
    private final IMcpGatewayToolDao mcpGatewayToolDao;

    public GatewayRepository(IMcpGatewayDao mcpGatewayDao, IMcpGatewayToolDao mcpGatewayToolDao) {
        this.mcpGatewayDao = mcpGatewayDao;
        this.mcpGatewayToolDao = mcpGatewayToolDao;
    }

    @Override
    public void saveGatewayConfig(GatewayConfigCommandEntity commandEntity) {
        GatewayConfigVO gatewayConfigVO = commandEntity.getGatewayConfigVO();

        McpGatewayPO mcpGatewayPO = new McpGatewayPO();
        mcpGatewayPO.setGatewayId(gatewayConfigVO.getGatewayId());
        mcpGatewayPO.setGatewayName(gatewayConfigVO.getGatewayName());
        mcpGatewayPO.setGatewayDesc(gatewayConfigVO.getGatewayDesc());
        mcpGatewayPO.setVersion(gatewayConfigVO.getVersion());
        mcpGatewayPO.setAuth(gatewayConfigVO.getAuth() == null ? GatewayEnum.GatewayAuthStatusEnum.ENABLE.getCode() : gatewayConfigVO.getAuth().getCode());
        mcpGatewayPO.setStatus(gatewayConfigVO.getStatus() == null ? GatewayEnum.GatewayStatus.NOT_VERIFIED.getCode() : gatewayConfigVO.getStatus().getCode());
        mcpGatewayDao.insert(mcpGatewayPO);
    }

    @Override
    public void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity) {
        GatewayConfigVO gatewayConfigVO = commandEntity.getGatewayConfigVO();
        if (gatewayConfigVO.getAuth() == null) {
            return;
        }

        McpGatewayPO mcpGatewayPO = new McpGatewayPO();
        mcpGatewayPO.setGatewayId(gatewayConfigVO.getGatewayId());
        mcpGatewayPO.setAuth(gatewayConfigVO.getAuth().getCode());
        int count = mcpGatewayDao.updateAuthStatusByGatewayId(mcpGatewayPO);
        if (count != 1) {
            throw new AppException(ResponseCode.DB_UPDATE_FAIL.getCode(), ResponseCode.DB_UPDATE_FAIL.getInfo());
        }
    }

    @Override
    public void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity) {
        GatewayToolConfigVO gatewayToolConfigVO = commandEntity.getGatewayToolConfigVO();

        McpGatewayToolPO mcpGatewayToolPO = new McpGatewayToolPO();
        mcpGatewayToolPO.setGatewayId(gatewayToolConfigVO.getGatewayId());
        mcpGatewayToolPO.setToolId(gatewayToolConfigVO.getToolId());
        mcpGatewayToolPO.setToolName(gatewayToolConfigVO.getToolName());
        mcpGatewayToolPO.setToolType(gatewayToolConfigVO.getToolType());
        mcpGatewayToolPO.setToolDescription(gatewayToolConfigVO.getToolDescription());
        mcpGatewayToolPO.setToolVersion(gatewayToolConfigVO.getToolVersion());
        mcpGatewayToolPO.setProtocolId(gatewayToolConfigVO.getProtocolId());
        mcpGatewayToolPO.setProtocolType(gatewayToolConfigVO.getProtocolType());
        mcpGatewayToolDao.insert(mcpGatewayToolPO);
    }

    @Override
    public void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity) {
        GatewayToolConfigVO gatewayToolConfigVO = commandEntity.getGatewayToolConfigVO();

        McpGatewayToolPO mcpGatewayToolPO = new McpGatewayToolPO();
        mcpGatewayToolPO.setGatewayId(gatewayToolConfigVO.getGatewayId());
        mcpGatewayToolPO.setProtocolId(gatewayToolConfigVO.getProtocolId());
        mcpGatewayToolPO.setProtocolType(gatewayToolConfigVO.getProtocolType());
        int count = mcpGatewayToolDao.updateProtocolByGatewayId(mcpGatewayToolPO);
        if (count != 1) {
            throw new AppException(ResponseCode.DB_UPDATE_FAIL.getCode(), ResponseCode.DB_UPDATE_FAIL.getInfo());
        }
    }
}
