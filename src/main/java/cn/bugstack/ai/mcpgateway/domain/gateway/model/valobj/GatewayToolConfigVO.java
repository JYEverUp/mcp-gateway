package cn.bugstack.ai.mcpgateway.domain.gateway.model.valobj;

public class GatewayToolConfigVO {

    private final String gatewayId;
    private final Long toolId;
    private final String toolName;
    private final String toolType;
    private final String toolDescription;
    private final String toolVersion;
    private final Long protocolId;
    private final String protocolType;

    public GatewayToolConfigVO(
            String gatewayId,
            Long toolId,
            String toolName,
            String toolType,
            String toolDescription,
            String toolVersion,
            Long protocolId,
            String protocolType) {
        this.gatewayId = gatewayId;
        this.toolId = toolId;
        this.toolName = toolName;
        this.toolType = toolType;
        this.toolDescription = toolDescription;
        this.toolVersion = toolVersion;
        this.protocolId = protocolId;
        this.protocolType = protocolType;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public Long getToolId() {
        return toolId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolType() {
        return toolType;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public Long getProtocolId() {
        return protocolId;
    }

    public String getProtocolType() {
        return protocolType;
    }
}
