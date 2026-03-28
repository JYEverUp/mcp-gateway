package cn.bugstack.ai.mcpgateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAccessInterceptor apiKeyAccessInterceptor;

    public WebMvcConfig(ApiKeyAccessInterceptor apiKeyAccessInterceptor) {
        this.apiKeyAccessInterceptor = apiKeyAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAccessInterceptor)
                .addPathPatterns(
                        "/custom-mcp/**",
                        "/*/mcp/sse",
                        "/debug/mcp/*/message",
                        "/debug/mcp/*/session/*")
                .excludePathPatterns(
                        "/api/health",
                        "/debug/mcp/health");
    }
}
