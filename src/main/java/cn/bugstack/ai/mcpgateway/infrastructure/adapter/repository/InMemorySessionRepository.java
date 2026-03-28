package cn.bugstack.ai.mcpgateway.infrastructure.adapter.repository;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InMemorySessionRepository implements ISessionRepository {

    private static final String GATEWAY_ID = "gateway_001";

    private final Map<String, McpGatewayConfigVO> gateways;
    private final Map<String, List<McpToolConfigVO>> toolsByGateway;

    public InMemorySessionRepository() {
        McpGatewayConfigVO gateway = new McpGatewayConfigVO(
                GATEWAY_ID,
                "mcp-gateway-demo",
                "A lightweight MCP gateway sample with session, tool registry and protocol dispatch.",
                "1.0.0");

        McpToolConfigVO echoTool = new McpToolConfigVO(
                GATEWAY_ID,
                1001L,
                "echo_payload",
                "Echo request content and metadata through the gateway protocol adapter.",
                "1.0.0",
                new McpToolProtocolConfigVO(
                        new McpToolProtocolConfigVO.HTTPConfig(
                                "http://localhost:8080/mock/tools/echo",
                                "{\"X-Gateway-Source\":\"mcp-gateway\"}",
                                "POST",
                                30000),
                        List.of(
                                new McpToolProtocolConfigVO.ProtocolMapping("request", null, "payload", "payload", "object", "Payload to echo back", 1, 1),
                                new McpToolProtocolConfigVO.ProtocolMapping("request", "payload", "message", "payload.message", "string", "Echo message", 1, 1),
                                new McpToolProtocolConfigVO.ProtocolMapping("request", "payload", "traceId", "payload.traceId", "string", "Business trace id", 0, 2))));

        McpToolConfigVO profileTool = new McpToolConfigVO(
                GATEWAY_ID,
                1002L,
                "fetch_profile",
                "Query a simple profile from an HTTP GET endpoint with path and query binding.",
                "1.0.0",
                new McpToolProtocolConfigVO(
                        new McpToolProtocolConfigVO.HTTPConfig(
                                "http://localhost:8080/mock/tools/profile/{userId}",
                                "{}",
                                "GET",
                                30000),
                        List.of(
                                new McpToolProtocolConfigVO.ProtocolMapping("request", null, "userId", "userId", "string", "User id in path", 1, 1),
                                new McpToolProtocolConfigVO.ProtocolMapping("request", null, "verbose", "verbose", "boolean", "Whether to include verbose fields", 0, 2))));

        this.gateways = Map.of(GATEWAY_ID, gateway);
        this.toolsByGateway = Map.of(GATEWAY_ID, List.of(echoTool, profileTool));
    }

    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        return gateways.get(gatewayId);
    }

    @Override
    public List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        return toolsByGateway.getOrDefault(gatewayId, List.of());
    }

    @Override
    public McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName) {
        return queryMcpGatewayToolConfigListByGatewayId(gatewayId).stream()
                .filter(tool -> tool.getToolName().equals(toolName))
                .findFirst()
                .map(McpToolConfigVO::getMcpToolProtocolConfigVO)
                .orElse(null);
    }

}
