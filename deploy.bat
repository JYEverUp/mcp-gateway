@echo off
echo [mcp-gateway] starting docker compose deployment...
docker compose up -d --build
echo [mcp-gateway] deployment submitted.
echo [mcp-gateway] app health: http://localhost:8080/api/health
