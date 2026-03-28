package cn.bugstack.ai.mcpgateway.cases.mcp;

import cn.bugstack.ai.mcpgateway.domain.session.model.entity.HandleMessageCommandEntity;
import org.springframework.http.ResponseEntity;

public interface IMcpMessageService {

    ResponseEntity<Void> handleMessage(HandleMessageCommandEntity commandEntity) throws Exception;

}
