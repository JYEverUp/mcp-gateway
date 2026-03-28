package cn.bugstack.ai.mcpgateway.domain.session.service.management;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SessionManagementService implements ISessionManagementService {

    private static final long SESSION_TIMEOUT_MINUTES = 30;

    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, SessionConfigVO> activeSessions = new ConcurrentHashMap<>();

    public SessionManagementService() {
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public SessionConfigVO createSession(String gatewayId, String apiKey) {
        String sessionId = UUID.randomUUID().toString();
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();

        String messageEndpoint = "/custom-mcp/message?gatewayId=" + gatewayId + "&sessionId=" + sessionId;
        if (apiKey != null && !apiKey.isBlank()) {
            messageEndpoint += "&api_key=" + apiKey;
        }

        sink.tryEmitNext(ServerSentEvent.<String>builder()
                .event("endpoint")
                .data(messageEndpoint)
                .build());

        SessionConfigVO session = new SessionConfigVO(sessionId, sink);
        activeSessions.put(sessionId, session);
        return session;
    }

    @Override
    public void removeSession(String sessionId) {
        SessionConfigVO session = activeSessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.markInactive();
        session.getSink().tryEmitComplete();
    }

    @Override
    public SessionConfigVO getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        SessionConfigVO session = activeSessions.get(sessionId);
        if (session != null && session.isActive()) {
            session.updateLastAccessed();
            return session;
        }
        return null;
    }

    public void cleanupExpiredSessions() {
        for (SessionConfigVO session : activeSessions.values()) {
            if (!session.isActive() || session.isExpired(SESSION_TIMEOUT_MINUTES)) {
                removeSession(session.getSessionId());
            }
        }
    }

    @Override
    public void shutdown() {
        for (String sessionId : activeSessions.keySet()) {
            removeSession(sessionId);
        }
        cleanupScheduler.shutdownNow();
    }

}
