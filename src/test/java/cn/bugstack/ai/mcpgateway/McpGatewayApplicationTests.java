package cn.bugstack.ai.mcpgateway;

import cn.bugstack.ai.mcpgateway.domain.session.model.valobj.SessionConfigVO;
import cn.bugstack.ai.mcpgateway.domain.session.service.ISessionManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpGatewayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ISessionManagementService sessionManagementService;

    @Test
    void healthEndpointLoads() throws Exception {
        mockMvc.perform(get("/debug/mcp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.defaultGatewayId").value("gateway_001"));
    }

    @Test
    void customSessionEndpointReturnsSessionInfo() throws Exception {
        SessionConfigVO session = sessionManagementService.createSession("gateway_001", null);

        mockMvc.perform(get("/custom-mcp/session")
                        .param("gatewayId", "gateway_001")
                        .param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayId").value("gateway_001"))
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()));
    }

    @Test
    void registerToolAppearsInToolList() throws Exception {
        String requestBody = """
                {
                  "gatewayId": "gateway_001",
                  "toolName": "lookup_city_weather",
                  "toolDescription": "Lookup weather by city name",
                  "toolVersion": "1.0.0",
                  "httpUrl": "http://localhost:8080/mock/tools/echo",
                  "httpMethod": "POST",
                  "mappings": [
                    {
                      "fieldName": "payload",
                      "mcpPath": "payload",
                      "mcpType": "object",
                      "isRequired": 1,
                      "sortOrder": 1
                    },
                    {
                      "parentPath": "payload",
                      "fieldName": "city",
                      "mcpPath": "payload.city",
                      "mcpType": "string",
                      "mcpDesc": "City name",
                      "isRequired": 1,
                      "sortOrder": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/custom-mcp/tool/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolName").value("lookup_city_weather"));

        mockMvc.perform(get("/custom-mcp/tool/list")
                        .param("gatewayId", "gateway_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.toolName=='lookup_city_weather')]").exists());
    }

    @Test
    void registerGatewayAndUpdateAuth() throws Exception {
        String registerBody = """
                {
                  "gatewayId": "gateway_003",
                  "gatewayName": "Employee Gateway",
                  "gatewayDesc": "Employee info gateway",
                  "version": "1.0.0",
                  "auth": 1,
                  "status": 1
                }
                """;

        mockMvc.perform(post("/custom-mcp/gateway/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayId").value("gateway_003"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(get("/custom-mcp/gateway/detail")
                        .param("gatewayId", "gateway_003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayId").value("gateway_003"))
                .andExpect(jsonPath("$.auth").value(1));

        String authBody = """
                {
                  "gatewayId": "gateway_003",
                  "auth": 0
                }
                """;

        mockMvc.perform(post("/custom-mcp/gateway/auth/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true))
                .andExpect(jsonPath("$.auth").value(0));

        mockMvc.perform(get("/custom-mcp/gateway/detail")
                        .param("gatewayId", "gateway_003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayId").value("gateway_003"))
                .andExpect(jsonPath("$.auth").value(0));
    }

}
