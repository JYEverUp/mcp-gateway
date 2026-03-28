package cn.bugstack.ai.mcpgateway.domain.gateway.model.entity;

import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayToolConfigVO;

public class GatewayToolConfigCommandEntity {

    private GatewayToolConfigVO gatewayToolConfigVO;

    public static GatewayToolConfigCommandEntity buildUpdateGatewayProtocol(String gatewayId, Long toolId, Long protocolId, String protocolType) {
        GatewayToolConfigCommandEntity entity = new GatewayToolConfigCommandEntity();
        entity.setGatewayToolConfigVO(new GatewayToolConfigVO(gatewayId, toolId, null, null, null, null, protocolId, protocolType));
        return entity;
    }

    public GatewayToolConfigVO getGatewayToolConfigVO() {
        return gatewayToolConfigVO;
    }

    public void setGatewayToolConfigVO(GatewayToolConfigVO gatewayToolConfigVO) {
        this.gatewayToolConfigVO = gatewayToolConfigVO;
    }
}
