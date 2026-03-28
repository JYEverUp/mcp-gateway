INSERT INTO mcp_gateway (gateway_id, gateway_name, gateway_desc, version, auth, status)
VALUES ('gateway_001', 'mcp-gateway-demo', 'A lightweight MCP gateway sample with session, tool registry and protocol dispatch.', '1.0.0', 0, 1);

INSERT INTO mcp_protocol_http (protocol_id, http_url, http_method, http_headers, timeout, retry_times, status)
VALUES (1001, 'http://localhost:8080/mock/tools/echo', 'POST', '{"X-Gateway-Source":"mcp-gateway"}', 30000, 0, 1);

INSERT INTO mcp_protocol_http (protocol_id, http_url, http_method, http_headers, timeout, retry_times, status)
VALUES (1002, 'http://localhost:8080/mock/tools/profile/{userId}', 'GET', '{}', 30000, 0, 1);

INSERT INTO mcp_protocol_mapping (protocol_id, mapping_type, parent_path, field_name, mcp_path, mcp_type, mcp_desc, is_required, sort_order)
VALUES (1001, 'request', NULL, 'payload', 'payload', 'object', 'Payload to echo back', 1, 1);
INSERT INTO mcp_protocol_mapping (protocol_id, mapping_type, parent_path, field_name, mcp_path, mcp_type, mcp_desc, is_required, sort_order)
VALUES (1001, 'request', 'payload', 'message', 'payload.message', 'string', 'Echo message', 1, 1);
INSERT INTO mcp_protocol_mapping (protocol_id, mapping_type, parent_path, field_name, mcp_path, mcp_type, mcp_desc, is_required, sort_order)
VALUES (1001, 'request', 'payload', 'traceId', 'payload.traceId', 'string', 'Business trace id', 0, 2);

INSERT INTO mcp_protocol_mapping (protocol_id, mapping_type, parent_path, field_name, mcp_path, mcp_type, mcp_desc, is_required, sort_order)
VALUES (1002, 'request', NULL, 'userId', 'userId', 'string', 'User id in path', 1, 1);
INSERT INTO mcp_protocol_mapping (protocol_id, mapping_type, parent_path, field_name, mcp_path, mcp_type, mcp_desc, is_required, sort_order)
VALUES (1002, 'request', NULL, 'verbose', 'verbose', 'boolean', 'Whether to include verbose fields', 0, 2);

INSERT INTO mcp_gateway_tool (gateway_id, tool_id, tool_name, tool_type, tool_description, tool_version, protocol_id, protocol_type)
VALUES ('gateway_001', 1001, 'echo_payload', 'function', 'Echo request content and metadata through the gateway protocol adapter.', '1.0.0', 1001, 'http');
INSERT INTO mcp_gateway_tool (gateway_id, tool_id, tool_name, tool_type, tool_description, tool_version, protocol_id, protocol_type)
VALUES ('gateway_001', 1002, 'fetch_profile', 'function', 'Query a simple profile from an HTTP GET endpoint with path and query binding.', '1.0.0', 1002, 'http');
