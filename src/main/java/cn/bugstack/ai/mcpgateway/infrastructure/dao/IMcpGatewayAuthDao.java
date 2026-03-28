package cn.bugstack.ai.mcpgateway.infrastructure.dao;

import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayAuthPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IMcpGatewayAuthDao {

    int insert(McpGatewayAuthPO po);

    int deleteById(Long id);

    int updateById(McpGatewayAuthPO po);

    McpGatewayAuthPO queryById(Long id);

    List<McpGatewayAuthPO> queryAll();

    McpGatewayAuthPO queryMcpGatewayAuthPO(McpGatewayAuthPO req);

    int queryEffectiveGatewayAuthCount(String gatewayId);

}
