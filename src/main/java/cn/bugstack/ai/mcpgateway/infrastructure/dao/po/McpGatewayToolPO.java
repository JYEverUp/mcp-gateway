package cn.bugstack.ai.mcpgateway.infrastructure.dao.po;

import java.util.Date;

public class McpGatewayToolPO {

    private Long id;
    private String gatewayId;
    private Long toolId;
    private String toolName;
    private String toolType;
    private String toolDescription;
    private String toolVersion;
    private Long protocolId;
    private String protocolType;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGatewayId() { return gatewayId; }
    public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolType() { return toolType; }
    public void setToolType(String toolType) { this.toolType = toolType; }
    public String getToolDescription() { return toolDescription; }
    public void setToolDescription(String toolDescription) { this.toolDescription = toolDescription; }
    public String getToolVersion() { return toolVersion; }
    public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }
    public Long getProtocolId() { return protocolId; }
    public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }
    public String getProtocolType() { return protocolType; }
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

}
