package cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj;

import cn.bugstack.ai.mcpgateway.types.enums.GatewayEnum;

public class GatewayConfigVO {

    private final String gatewayId;
    private final String gatewayName;
    private final String gatewayDesc;
    private final String version;
    private final GatewayEnum.GatewayAuthStatusEnum auth;
    private final GatewayEnum.GatewayStatus status;

    public GatewayConfigVO(
            String gatewayId,
            String gatewayName,
            String gatewayDesc,
            String version,
            GatewayEnum.GatewayAuthStatusEnum auth,
            GatewayEnum.GatewayStatus status) {
        this.gatewayId = gatewayId;
        this.gatewayName = gatewayName;
        this.gatewayDesc = gatewayDesc;
        this.version = version;
        this.auth = auth;
        this.status = status;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public String getGatewayDesc() {
        return gatewayDesc;
    }

    public String getVersion() {
        return version;
    }

    public GatewayEnum.GatewayAuthStatusEnum getAuth() {
        return auth;
    }

    public GatewayEnum.GatewayStatus getStatus() {
        return status;
    }
}
