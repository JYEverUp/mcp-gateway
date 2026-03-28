package cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.impl;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.McpSchemaVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.message.handler.IRequestHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("toolsListHandler")
public class ToolsListHandler implements IRequestHandler {

    private final ISessionRepository repository;

    public ToolsListHandler(ISessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        List<McpToolConfigVO> toolConfigs = repository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);
        return new McpSchemaVO.JSONRPCResponse(
                McpSchemaVO.JSONRPC_VERSION,
                message.id(),
                Map.of("tools", buildTools(toolConfigs)),
                null);
    }

    private List<McpSchemaVO.Tool> buildTools(List<McpToolConfigVO> toolConfigs) {
        List<McpSchemaVO.Tool> tools = new ArrayList<>();
        for (McpToolConfigVO toolConfig : toolConfigs) {
            List<McpToolProtocolConfigVO.ProtocolMapping> mappings =
                    new ArrayList<>(toolConfig.getMcpToolProtocolConfigVO().getRequestProtocolMappings());
            mappings.sort(Comparator.comparingInt(mapping -> mapping.getSortOrder() == null ? 0 : mapping.getSortOrder()));

            Map<String, List<McpToolProtocolConfigVO.ProtocolMapping>> childrenMap = new HashMap<>();
            List<McpToolProtocolConfigVO.ProtocolMapping> roots = new ArrayList<>();
            for (McpToolProtocolConfigVO.ProtocolMapping mapping : mappings) {
                if (mapping.getParentPath() == null) {
                    roots.add(mapping);
                } else {
                    childrenMap.computeIfAbsent(mapping.getParentPath(), key -> new ArrayList<>()).add(mapping);
                }
            }

            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            for (McpToolProtocolConfigVO.ProtocolMapping root : roots) {
                properties.put(root.getFieldName(), buildProperty(root, childrenMap));
                if (Integer.valueOf(1).equals(root.getIsRequired())) {
                    required.add(root.getFieldName());
                }
            }

            McpSchemaVO.JsonSchema schema = new McpSchemaVO.JsonSchema(
                    "object",
                    properties,
                    required.isEmpty() ? null : required,
                    false,
                    null,
                    null);
            tools.add(new McpSchemaVO.Tool(toolConfig.getToolName(), toolConfig.getToolDescription(), schema));
        }
        return tools;
    }

    private Map<String, Object> buildProperty(
            McpToolProtocolConfigVO.ProtocolMapping current,
            Map<String, List<McpToolProtocolConfigVO.ProtocolMapping>> childrenMap) {
        Map<String, Object> property = new HashMap<>();
        property.put("type", current.getMcpType());
        if (current.getMcpDesc() != null && !current.getMcpDesc().isBlank()) {
            property.put("description", current.getMcpDesc());
        }

        List<McpToolProtocolConfigVO.ProtocolMapping> children = childrenMap.get(current.getMcpPath());
        if (children != null && !children.isEmpty()) {
            children.sort(Comparator.comparingInt(mapping -> mapping.getSortOrder() == null ? 0 : mapping.getSortOrder()));
            Map<String, Object> nestedProperties = new HashMap<>();
            List<String> nestedRequired = new ArrayList<>();
            for (McpToolProtocolConfigVO.ProtocolMapping child : children) {
                nestedProperties.put(child.getFieldName(), buildProperty(child, childrenMap));
                if (Integer.valueOf(1).equals(child.getIsRequired())) {
                    nestedRequired.add(child.getFieldName());
                }
            }
            property.put("properties", nestedProperties);
            if (!nestedRequired.isEmpty()) {
                property.put("required", nestedRequired);
            }
        }

        return property;
    }

}
