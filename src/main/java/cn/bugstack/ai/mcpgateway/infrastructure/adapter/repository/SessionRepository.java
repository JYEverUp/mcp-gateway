package cn.bugstack.ai.mcpgateway.infrastructure.adapter.repository;

import cn.bugstack.ai.mcpgateway.domain.session.adapter.repository.ISessionRepository;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayToolDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpProtocolHttpDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpProtocolMappingDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayPO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayToolPO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpProtocolHttpPO;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpProtocolMappingPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SessionRepository implements ISessionRepository {

    private static final Logger log = LoggerFactory.getLogger(SessionRepository.class);

    private final IMcpGatewayDao mcpGatewayDao;
    private final IMcpGatewayToolDao mcpGatewayToolDao;
    private final IMcpProtocolHttpDao mcpProtocolHttpDao;
    private final IMcpProtocolMappingDao mcpProtocolMappingDao;

    public SessionRepository(
            IMcpGatewayDao mcpGatewayDao,
            IMcpGatewayToolDao mcpGatewayToolDao,
            IMcpProtocolHttpDao mcpProtocolHttpDao,
            IMcpProtocolMappingDao mcpProtocolMappingDao) {
        this.mcpGatewayDao = mcpGatewayDao;
        this.mcpGatewayToolDao = mcpGatewayToolDao;
        this.mcpProtocolHttpDao = mcpProtocolHttpDao;
        this.mcpProtocolMappingDao = mcpProtocolMappingDao;
    }

    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        log.info("query gateway config start, gatewayId={}", gatewayId);
        McpGatewayPO po = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        if (po == null) {
            log.info("query gateway config miss, gatewayId={}", gatewayId);
            return null;
        }
        McpGatewayConfigVO result = new McpGatewayConfigVO(po.getGatewayId(), po.getGatewayName(), po.getGatewayDesc(), po.getVersion());
        log.info("query gateway config success, gatewayId={}, gatewayName={}", gatewayId, po.getGatewayName());
        return result;
    }

    @Override
    public List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        log.info("query gateway tools start, gatewayId={}", gatewayId);
        List<McpToolConfigVO> result = new ArrayList<>();
        List<McpGatewayToolPO> toolPOList = mcpGatewayToolDao.queryEffectiveTools(gatewayId);
        for (McpGatewayToolPO toolPO : toolPOList) {
            List<McpProtocolMappingPO> mappingPOList = mcpProtocolMappingDao.queryMcpGatewayToolConfigListByProtocolId(toolPO.getProtocolId());
            List<McpToolProtocolConfigVO.ProtocolMapping> requestMappings = new ArrayList<>();
            for (McpProtocolMappingPO mappingPO : mappingPOList) {
                requestMappings.add(new McpToolProtocolConfigVO.ProtocolMapping(
                        mappingPO.getMappingType(),
                        mappingPO.getParentPath(),
                        mappingPO.getFieldName(),
                        mappingPO.getMcpPath(),
                        mappingPO.getMcpType(),
                        mappingPO.getMcpDesc(),
                        mappingPO.getIsRequired(),
                        mappingPO.getSortOrder()));
            }

            McpProtocolHttpPO httpPO = mcpProtocolHttpDao.queryMcpProtocolHttpByProtocolId(toolPO.getProtocolId());
            McpToolProtocolConfigVO.HTTPConfig httpConfig = null;
            if (httpPO != null) {
                httpConfig = new McpToolProtocolConfigVO.HTTPConfig(
                        httpPO.getHttpUrl(),
                        httpPO.getHttpHeaders(),
                        httpPO.getHttpMethod(),
                        httpPO.getTimeout());
            }

            result.add(new McpToolConfigVO(
                    toolPO.getGatewayId(),
                    toolPO.getToolId(),
                    toolPO.getToolName(),
                    toolPO.getToolDescription(),
                    toolPO.getToolVersion(),
                    new McpToolProtocolConfigVO(httpConfig, requestMappings)));
        }
        log.info("query gateway tools completed, gatewayId={}, toolCount={}", gatewayId, result.size());
        return result;
    }

    @Override
    public McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName) {
        log.info("query tool protocol start, gatewayId={}, toolName={}", gatewayId, toolName);
        McpGatewayToolPO req = new McpGatewayToolPO();
        req.setGatewayId(gatewayId);
        req.setToolName(toolName);
        Long protocolId = mcpGatewayToolDao.queryToolProtocolIdByToolName(req);
        if (protocolId == null) {
            log.info("query tool protocol miss, gatewayId={}, toolName={}", gatewayId, toolName);
            return null;
        }

        McpProtocolHttpPO httpPO = mcpProtocolHttpDao.queryMcpProtocolHttpByProtocolId(protocolId);
        if (httpPO == null) {
            log.info("query tool protocol http config miss, protocolId={}", protocolId);
            return null;
        }

        McpToolProtocolConfigVO result = new McpToolProtocolConfigVO(
                new McpToolProtocolConfigVO.HTTPConfig(
                        httpPO.getHttpUrl(),
                        httpPO.getHttpHeaders(),
                        httpPO.getHttpMethod(),
                        httpPO.getTimeout()),
                null);
        log.info("query tool protocol success, gatewayId={}, toolName={}, protocolId={}", gatewayId, toolName, protocolId);
        return result;
    }

    @Override
    @Transactional
    public McpToolConfigVO saveMcpToolConfig(McpToolConfigVO toolConfigVO) {
        log.info("save tool config start, gatewayId={}, toolId={}, toolName={}",
                toolConfigVO.getGatewayId(), toolConfigVO.getToolId(), toolConfigVO.getToolName());
        ensureGatewayExists(toolConfigVO.getGatewayId());

        long protocolId = toolConfigVO.getToolId();
        McpToolProtocolConfigVO protocolConfig = toolConfigVO.getMcpToolProtocolConfigVO();

        McpProtocolHttpPO httpPO = new McpProtocolHttpPO();
        httpPO.setProtocolId(protocolId);
        httpPO.setHttpUrl(protocolConfig.getHttpConfig().getHttpUrl());
        httpPO.setHttpMethod(protocolConfig.getHttpConfig().getHttpMethod());
        httpPO.setHttpHeaders(protocolConfig.getHttpConfig().getHttpHeaders());
        httpPO.setTimeout(protocolConfig.getHttpConfig().getTimeout());
        httpPO.setRetryTimes(0);
        httpPO.setStatus(1);
        mcpProtocolHttpDao.insert(httpPO);

        for (McpToolProtocolConfigVO.ProtocolMapping mapping : protocolConfig.getRequestProtocolMappings()) {
            McpProtocolMappingPO mappingPO = new McpProtocolMappingPO();
            mappingPO.setProtocolId(protocolId);
            mappingPO.setMappingType(mapping.getMappingType());
            mappingPO.setParentPath(mapping.getParentPath());
            mappingPO.setFieldName(mapping.getFieldName());
            mappingPO.setMcpPath(mapping.getMcpPath());
            mappingPO.setMcpType(mapping.getMcpType());
            mappingPO.setMcpDesc(mapping.getMcpDesc());
            mappingPO.setIsRequired(mapping.getIsRequired());
            mappingPO.setSortOrder(mapping.getSortOrder());
            mcpProtocolMappingDao.insert(mappingPO);
        }

        McpGatewayToolPO toolPO = new McpGatewayToolPO();
        toolPO.setGatewayId(toolConfigVO.getGatewayId());
        toolPO.setToolId(toolConfigVO.getToolId());
        toolPO.setToolName(toolConfigVO.getToolName());
        toolPO.setToolType("function");
        toolPO.setToolDescription(toolConfigVO.getToolDescription());
        toolPO.setToolVersion(toolConfigVO.getToolVersion());
        toolPO.setProtocolId(protocolId);
        toolPO.setProtocolType("http");
        mcpGatewayToolDao.insert(toolPO);

        log.info("save tool config completed, gatewayId={}, toolId={}, mappingCount={}",
                toolConfigVO.getGatewayId(),
                toolConfigVO.getToolId(),
                protocolConfig.getRequestProtocolMappings() == null ? 0 : protocolConfig.getRequestProtocolMappings().size());
        return toolConfigVO;
    }

    @Override
    public List<Map<String, Object>> queryToolDetailsByGatewayId(String gatewayId) {
        log.info("query tool details start, gatewayId={}", gatewayId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (McpToolConfigVO tool : queryMcpGatewayToolConfigListByGatewayId(gatewayId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gatewayId", tool.getGatewayId());
            item.put("toolId", tool.getToolId());
            item.put("toolName", tool.getToolName());
            item.put("toolDescription", tool.getToolDescription());
            item.put("toolVersion", tool.getToolVersion());
            item.put("httpConfig", tool.getMcpToolProtocolConfigVO().getHttpConfig());
            item.put("mappings", tool.getMcpToolProtocolConfigVO().getRequestProtocolMappings());
            result.add(item);
        }
        log.info("query tool details completed, gatewayId={}, toolCount={}", gatewayId, result.size());
        return result;
    }

    private void ensureGatewayExists(String gatewayId) {
        if (mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId) != null) {
            log.info("gateway already exists, gatewayId={}", gatewayId);
            return;
        }

        McpGatewayPO gatewayPO = new McpGatewayPO();
        gatewayPO.setGatewayId(gatewayId);
        gatewayPO.setGatewayName(gatewayId);
        gatewayPO.setGatewayDesc("Dynamic gateway generated by tool registration.");
        gatewayPO.setVersion("1.0.0");
        gatewayPO.setAuth(0);
        gatewayPO.setStatus(1);
        mcpGatewayDao.insert(gatewayPO);
        log.info("gateway auto-created for tool registration, gatewayId={}", gatewayId);
    }

}
