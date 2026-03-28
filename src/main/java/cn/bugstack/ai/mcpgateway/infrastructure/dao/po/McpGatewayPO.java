package cn.bugstack.ai.mcpgateway.infrastructure.dao.po;

import java.util.Date;

public class McpGatewayPO {

    private Long id;
    private String gatewayId;
    private String gatewayName;
    private String gatewayDesc;
    private String version;
    private Integer status;
    private Integer auth;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGatewayId() { return gatewayId; }
    public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
    public String getGatewayName() { return gatewayName; }
    public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }
    public String getGatewayDesc() { return gatewayDesc; }
    public void setGatewayDesc(String gatewayDesc) { this.gatewayDesc = gatewayDesc; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getAuth() { return auth; }
    public void setAuth(Integer auth) { this.auth = auth; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

}
