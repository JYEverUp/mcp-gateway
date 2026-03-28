package cn.bugstack.ai.mcpgateway.domain.session.adapter.port;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;

import java.io.IOException;
import java.util.Map;

public interface ISessionPort {

    Object toolCall(McpToolProtocolConfigVO.HTTPConfig httpConfig, Map<String, Object> params) throws IOException;

}
