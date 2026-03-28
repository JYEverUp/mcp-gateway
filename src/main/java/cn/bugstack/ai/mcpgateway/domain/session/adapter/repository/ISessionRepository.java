package cn.bugstack.ai.mcpgateway.domain.session.adapter.repository;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;

import java.util.List;

public interface ISessionRepository {

    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);

    List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId);

    McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName);

}
