package cn.bugstack.ai.mcpgateway.domain.session.model.valobj.gateway;

import java.util.List;

public class McpToolProtocolConfigVO {

    private final HTTPConfig httpConfig;
    private final List<ProtocolMapping> requestProtocolMappings;

    public McpToolProtocolConfigVO(HTTPConfig httpConfig, List<ProtocolMapping> requestProtocolMappings) {
        this.httpConfig = httpConfig;
        this.requestProtocolMappings = requestProtocolMappings;
    }

    public HTTPConfig getHttpConfig() {
        return httpConfig;
    }

    public List<ProtocolMapping> getRequestProtocolMappings() {
        return requestProtocolMappings;
    }

    public static class HTTPConfig {
        private final String httpUrl;
        private final String httpHeaders;
        private final String httpMethod;
        private final Integer timeout;

        public HTTPConfig(String httpUrl, String httpHeaders, String httpMethod, Integer timeout) {
            this.httpUrl = httpUrl;
            this.httpHeaders = httpHeaders;
            this.httpMethod = httpMethod;
            this.timeout = timeout;
        }

        public String getHttpUrl() {
            return httpUrl;
        }

        public String getHttpHeaders() {
            return httpHeaders;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public Integer getTimeout() {
            return timeout;
        }
    }

    public static class ProtocolMapping {
        private final String mappingType;
        private final String parentPath;
        private final String fieldName;
        private final String mcpPath;
        private final String mcpType;
        private final String mcpDesc;
        private final Integer isRequired;
        private final Integer sortOrder;

        public ProtocolMapping(
                String mappingType,
                String parentPath,
                String fieldName,
                String mcpPath,
                String mcpType,
                String mcpDesc,
                Integer isRequired,
                Integer sortOrder) {
            this.mappingType = mappingType;
            this.parentPath = parentPath;
            this.fieldName = fieldName;
            this.mcpPath = mcpPath;
            this.mcpType = mcpType;
            this.mcpDesc = mcpDesc;
            this.isRequired = isRequired;
            this.sortOrder = sortOrder;
        }

        public String getMappingType() {
            return mappingType;
        }

        public String getParentPath() {
            return parentPath;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getMcpPath() {
            return mcpPath;
        }

        public String getMcpType() {
            return mcpType;
        }

        public String getMcpDesc() {
            return mcpDesc;
        }

        public Integer getIsRequired() {
            return isRequired;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }
    }

}
