package cn.bugstack.ai.mcpgateway.infrastructure.dao;

import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpAiModelConfigPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IMcpAiModelConfigDao {

    List<McpAiModelConfigPO> queryAllActive();

    McpAiModelConfigPO queryByConfigId(String configId);

    McpAiModelConfigPO queryDefaultConfig();
}
