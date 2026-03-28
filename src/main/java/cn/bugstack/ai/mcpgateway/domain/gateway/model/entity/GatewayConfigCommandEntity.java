package cn.bugstack.ai.mcpgateway.domain.gateway.model.entity;

import cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj.GatewayConfigVO;
import cn.bugstack.ai.mcpgateway.types.enums.GatewayEnum;

public class GatewayConfigCommandEntity {

    private GatewayConfigVO gatewayConfigVO;

    public static GatewayConfigCommandEntity buildUpdateGatewayAuthStatusVO(String gatewayId, GatewayEnum.GatewayAuthStatusEnum auth) {
        GatewayConfigCommandEntity entity = new GatewayConfigCommandEntity();
        entity.setGatewayConfigVO(new GatewayConfigVO(gatewayId, null, null, null, auth, null));
        return entity;
    }

    public GatewayConfigVO getGatewayConfigVO() {
        return gatewayConfigVO;
    }

    public void setGatewayConfigVO(GatewayConfigVO gatewayConfigVO) {
        this.gatewayConfigVO = gatewayConfigVO;
    }
}
