package cn.bugstack.ai.mcpgateway.domain.session.model.valobj;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SessionConfigVO {

    private final String sessionId;
    private final Sinks.Many<ServerSentEvent<String>> sink;
    private final Instant createTime;
    private volatile Instant lastAccessedTime;
    private volatile boolean active;

    public SessionConfigVO(String sessionId, Sinks.Many<ServerSentEvent<String>> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
        this.createTime = Instant.now();
        this.lastAccessedTime = this.createTime;
        this.active = true;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Sinks.Many<ServerSentEvent<String>> getSink() {
        return sink;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getLastAccessedTime() {
        return lastAccessedTime;
    }

    public boolean isActive() {
        return active;
    }

    public void markInactive() {
        this.active = false;
    }

    public void updateLastAccessed() {
        this.lastAccessedTime = Instant.now();
    }

    public boolean isExpired(long timeoutMinutes) {
        return lastAccessedTime.isBefore(Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES));
    }

}
