package cn.bugstack.ai.mcpgateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "mcp-gateway");
        response.put("status", "UP");
        response.put("timestamp", OffsetDateTime.now().toString());
        log.info("health endpoint invoked, response={}", response);
        return response;
    }

}
