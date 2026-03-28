package cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway;

public class McpToolConfigVO {

    private final String gatewayId;
    private final Long toolId;
    private final String toolName;
    private final String toolDescription;
    private final String toolVersion;
    private final McpToolProtocolConfigVO mcpToolProtocolConfigVO;

    public McpToolConfigVO(
            String gatewayId,
            Long toolId,
            String toolName,
            String toolDescription,
            String toolVersion,
            McpToolProtocolConfigVO mcpToolProtocolConfigVO) {
        this.gatewayId = gatewayId;
        this.toolId = toolId;
        this.toolName = toolName;
        this.toolDescription = toolDescription;
        this.toolVersion = toolVersion;
        this.mcpToolProtocolConfigVO = mcpToolProtocolConfigVO;
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

    public String getToolDescription() {
        return toolDescription;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public McpToolProtocolConfigVO getMcpToolProtocolConfigVO() {
        return mcpToolProtocolConfigVO;
    }

}
