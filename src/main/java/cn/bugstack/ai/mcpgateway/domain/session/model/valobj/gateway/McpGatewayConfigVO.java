package cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway;

public class McpGatewayConfigVO {

    private final String gatewayId;
    private final String gatewayName;
    private final String gatewayDesc;
    private final String version;

    public McpGatewayConfigVO(String gatewayId, String gatewayName, String gatewayDesc, String version) {
        this.gatewayId = gatewayId;
        this.gatewayName = gatewayName;
        this.gatewayDesc = gatewayDesc;
        this.version = version;
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

}
