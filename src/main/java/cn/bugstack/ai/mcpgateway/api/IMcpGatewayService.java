package cn.bugstack.ai.mcpgateway.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IMcpGatewayService {

    Flux<ServerSentEvent<String>> handleSseConnection(String gatewayId, String apiKey);

    Mono<ResponseEntity<Void>> handleMessage(String gatewayId, String sessionId, String apiKey, String messageBody);

}
