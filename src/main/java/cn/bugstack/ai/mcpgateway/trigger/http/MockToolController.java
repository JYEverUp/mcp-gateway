package cn.bugstack.ai.mcpgateway.trigger.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/mock/tools")
public class MockToolController {

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "echo_payload");
        result.put("received", payload);
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    @GetMapping("/profile/{userId}")
    public Map<String, Object> profile(
            @PathVariable("userId") String userId,
            @RequestParam(value = "verbose", defaultValue = "false") boolean verbose) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "fetch_profile");
        result.put("userId", userId);
        result.put("role", "gateway-demo-user");
        result.put("verbose", verbose);
        result.put("timestamp", Instant.now().toString());
        if (verbose) {
            result.put("tags", new String[]{"mcp", "gateway", "demo"});
        }
        return result;
    }

}
