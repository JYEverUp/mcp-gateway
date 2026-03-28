package cn.bugstack.ai.mcpgateway.infrastructure.dao;

import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayToolPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IMcpGatewayToolDao {

    int insert(McpGatewayToolPO po);

    int updateProtocolByGatewayId(McpGatewayToolPO po);

    List<McpGatewayToolPO> queryEffectiveTools(String gatewayId);

    Long queryToolProtocolIdByToolName(McpGatewayToolPO mcpGatewayToolPOReq);

}
