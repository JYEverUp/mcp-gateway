DROP TABLE IF EXISTS mcp_gateway_auth;
DROP TABLE IF EXISTS mcp_gateway_tool;
DROP TABLE IF EXISTS mcp_protocol_http;
DROP TABLE IF EXISTS mcp_protocol_mapping;
DROP TABLE IF EXISTS mcp_gateway;

CREATE TABLE mcp_gateway (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL UNIQUE,
    gateway_name VARCHAR(128) NOT NULL,
    gateway_desc VARCHAR(512),
    version VARCHAR(32),
    auth INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_gateway_auth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL,
    api_key VARCHAR(128) NOT NULL,
    rate_limit INT,
    expire_time TIMESTAMP,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_protocol_http (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocol_id BIGINT NOT NULL,
    http_url VARCHAR(512) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    http_headers CLOB,
    timeout INT,
    retry_times INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_protocol_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocol_id BIGINT NOT NULL,
    mapping_type VARCHAR(32) DEFAULT 'request',
    parent_path VARCHAR(255),
    field_name VARCHAR(128) NOT NULL,
    mcp_path VARCHAR(255) NOT NULL,
    mcp_type VARCHAR(32) NOT NULL,
    mcp_desc VARCHAR(512),
    is_required INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_gateway_tool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL,
    tool_id BIGINT NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    tool_type VARCHAR(32) DEFAULT 'function',
    tool_description VARCHAR(512),
    tool_version VARCHAR(32),
    protocol_id BIGINT NOT NULL,
    protocol_type VARCHAR(32) DEFAULT 'http',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
