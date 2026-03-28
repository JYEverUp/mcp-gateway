package cn.bugstack.ai.mcpgateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "mcp-gateway");
        response.put("status", "UP");
        response.put("timestamp", OffsetDateTime.now().toString());
        return response;
    }

}
