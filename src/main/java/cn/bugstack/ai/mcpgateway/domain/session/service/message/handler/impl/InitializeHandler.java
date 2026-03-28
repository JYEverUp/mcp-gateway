package cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.impl;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.IRequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service("initializeHandler")
public class InitializeHandler implements IRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(InitializeHandler.class);

    private final ISessionRepository repository;

    public InitializeHandler(ISessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        log.info("initialize handler invoked, gatewayId={}, requestId={}", gatewayId, message.id());
        McpSchemaVO.InitializeRequest request = McpSchemaVO.unmarshalFrom(message.params(), new TypeReference<>() {
        });
        McpGatewayConfigVO gatewayConfig = repository.queryMcpGatewayConfigByGatewayId(gatewayId);
        if (gatewayConfig == null) {
            log.warn("gateway config not found during initialize, gatewayId={}, fallback to default metadata", gatewayId);
            gatewayConfig = new McpGatewayConfigVO(
                    gatewayId,
                    gatewayId,
                    "Gateway config not found in persistence store, using fallback initialize metadata.",
                    "1.0.0");
        }

        McpSchemaVO.InitializeResult result = new McpSchemaVO.InitializeResult(
                request.protocolVersion() == null ? McpSchemaVO.LATEST_PROTOCOL_VERSION : request.protocolVersion(),
                new McpSchemaVO.ServerCapabilities(
                        new McpSchemaVO.ServerCapabilities.CompletionCapabilities(),
                        new HashMap<>(),
                        new McpSchemaVO.ServerCapabilities.LoggingCapabilities(),
                        new McpSchemaVO.ServerCapabilities.PromptCapabilities(true),
                        new McpSchemaVO.ServerCapabilities.ResourceCapabilities(false, true),
                        new McpSchemaVO.ServerCapabilities.ToolCapabilities(true)),
                new McpSchemaVO.Implementation(gatewayConfig.getGatewayName(), gatewayConfig.getVersion()),
                gatewayConfig.getGatewayDesc());

        log.info("initialize handler completed, gatewayId={}, serverName={}, version={}",
                gatewayId, gatewayConfig.getGatewayName(), gatewayConfig.getVersion());

        return new McpSchemaVO.JSONRPCResponse(McpSchemaVO.JSONRPC_VERSION, message.id(), result, null);
    }

}
