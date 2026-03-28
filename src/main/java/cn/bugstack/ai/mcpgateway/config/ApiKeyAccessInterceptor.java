package cn.bugstack.ai.mcpgateway.config;

import cn.bugstack.ai.mcpgateway.api.response.Response;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.IMcpGatewayAuthDao;
import cn.bugstack.ai.mcpgateway.infrastructure.dao.po.McpGatewayAuthPO;
import cn.bugstack.ai.mcpgateway.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ApiKeyAccessInterceptor implements HandlerInterceptor {

    private final IMcpGatewayAuthDao mcpGatewayAuthDao;
    private final ObjectMapper objectMapper;

    public ApiKeyAccessInterceptor(IMcpGatewayAuthDao mcpGatewayAuthDao, ObjectMapper objectMapper) {
        this.mcpGatewayAuthDao = mcpGatewayAuthDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = trimToNull(request.getParameter("api_key"));
        if (apiKey == null) {
            apiKey = trimToNull(request.getHeader("x-api-key"));
        }

        if (apiKey == null) {
            writeAuthError(response, "api_key 不能为空");
            return false;
        }

        String gatewayId = trimToNull(request.getParameter("gatewayId"));
        if (gatewayId == null) {
            Object uriVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriVars instanceof Map<?, ?> variables) {
                Object gatewayValue = variables.get("gatewayId");
                gatewayId = gatewayValue == null ? null : trimToNull(String.valueOf(gatewayValue));
            }
        }

        boolean authorized = gatewayId == null
                ? matchAnyActiveApiKey(apiKey)
                : matchGatewayApiKey(gatewayId, apiKey);

        if (!authorized) {
            writeAuthError(response, "API Key 无效或已过期");
            return false;
        }

        return true;
    }

    private boolean matchGatewayApiKey(String gatewayId, String apiKey) {
        McpGatewayAuthPO req = new McpGatewayAuthPO();
        req.setGatewayId(gatewayId);
        req.setApiKey(apiKey);
        McpGatewayAuthPO auth = mcpGatewayAuthDao.queryMcpGatewayAuthPO(req);
        return isActive(auth);
    }

    private boolean matchAnyActiveApiKey(String apiKey) {
        List<McpGatewayAuthPO> list = mcpGatewayAuthDao.queryAll();
        return list.stream().anyMatch(item -> apiKey.equals(item.getApiKey()) && isActive(item));
    }

    private boolean isActive(McpGatewayAuthPO auth) {
        if (auth == null) {
            return false;
        }
        if (auth.getStatus() == null || auth.getStatus() != 1) {
            return false;
        }
        Date expireTime = auth.getExpireTime();
        return expireTime == null || expireTime.after(new Date());
    }

    private void writeAuthError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Response.error(ResponseCode.AUTH_FAIL.getCode(), message)));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
