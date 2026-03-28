package cn.bugstack.ai.mcpgateway.cases.mcp;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface IMcpSessionService {

    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId, String apiKey);

}
