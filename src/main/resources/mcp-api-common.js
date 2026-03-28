(function () {
    const STORAGE_KEYS = {
        serverUrl: "mcp.api.serverUrl",
        gatewayId: "mcp.api.gatewayId",
        apiKey: "mcp.api.apiKey",
        sessionId: "mcp.api.sessionId",
        toolList: "mcp.api.toolList"
    };

    function save(key, value) {
        if (value === undefined || value === null) {
            return;
        }
        localStorage.setItem(key, value);
    }

    function load(key, fallback) {
        return localStorage.getItem(key) || fallback;
    }

    function trimSlash(value) {
        return (value || "").replace(/\/+$/, "");
    }

    function initBaseFields() {
        const defaults = {
            serverUrl: "http://localhost:8080",
            gatewayId: "gateway_001",
            apiKey: "test-api-key",
            sessionId: ""
        };

        setInputValue("server-url", load(STORAGE_KEYS.serverUrl, defaults.serverUrl));
        setInputValue("gateway-id", load(STORAGE_KEYS.gatewayId, defaults.gatewayId));
        setInputValue("api-key", load(STORAGE_KEYS.apiKey, defaults.apiKey));
        setInputValue("session-id", load(STORAGE_KEYS.sessionId, defaults.sessionId));
    }

    function setInputValue(id, value) {
        const element = document.getElementById(id);
        if (element && value !== null && value !== undefined) {
            element.value = value;
        }
    }

    function readBaseFields() {
        const base = {
            serverUrl: trimSlash(document.getElementById("server-url")?.value || ""),
            gatewayId: document.getElementById("gateway-id")?.value || "",
            apiKey: document.getElementById("api-key")?.value || "",
            sessionId: document.getElementById("session-id")?.value || ""
        };

        save(STORAGE_KEYS.serverUrl, base.serverUrl);
        save(STORAGE_KEYS.gatewayId, base.gatewayId);
        save(STORAGE_KEYS.apiKey, base.apiKey);
        save(STORAGE_KEYS.sessionId, base.sessionId);
        return base;
    }

    function buildUrl(path, searchParams) {
        const base = readBaseFields();
        const url = new URL(base.serverUrl + path);
        Object.entries(searchParams || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== "") {
                url.searchParams.set(key, value);
            }
        });
        return url.toString();
    }

    async function requestJson(url, options) {
        const response = await fetch(url, options);
        const text = await response.text();
        return {
            ok: response.ok,
            status: response.status,
            headers: response.headers,
            text,
            data: parseJsonSafe(text)
        };
    }

    function parseJsonSafe(text) {
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            return null;
        }
    }

    function logTo(logId, message, payload) {
        const el = document.getElementById(logId);
        if (!el) {
            return;
        }
        const now = new Date();
        const stamp = now.toTimeString().slice(0, 8);
        const content = payload === undefined
            ? `[${stamp}] ${message}`
            : `[${stamp}] ${message}: ${typeof payload === "string" ? payload : JSON.stringify(payload, null, 2)}`;
        el.textContent = `${content}\n\n${el.textContent}`.trim();
    }

    function updateStatus(id, title, lines) {
        const el = document.getElementById(id);
        if (!el) {
            return;
        }
        el.innerHTML = `<strong>${title}</strong>${lines.map((item) => `<div>${item}</div>`).join("")}`;
    }

    function updateSessionBox(id, base, endpoint) {
        const sessionId = parseSessionIdFromEndpoint(endpoint) || base.sessionId || "-";
        setInputValue("session-id", sessionId === "-" ? "" : sessionId);
        if (sessionId !== "-") {
            save(STORAGE_KEYS.sessionId, sessionId);
        }
        updateStatus(id, "会话信息", [
            `网关: ${base.gatewayId || "-"}`,
            `Session: ${sessionId}`,
            `Endpoint: ${endpoint || "-"}`
        ]);
    }

    function parseSessionIdFromEndpoint(endpoint) {
        if (!endpoint) {
            return "";
        }
        const match = endpoint.match(/sessionId=([^&]+)/);
        return match ? decodeURIComponent(match[1]) : "";
    }

    function ensureSession(logId) {
        const base = readBaseFields();
        if (!base.sessionId) {
            logTo(logId, "缺少 sessionId，请先建立连接");
            return null;
        }
        return base;
    }

    function getStoredTools() {
        const tools = parseJsonSafe(localStorage.getItem(STORAGE_KEYS.toolList) || "[]");
        return Array.isArray(tools) ? tools : [];
    }

    function setStoredTools(tools) {
        localStorage.setItem(STORAGE_KEYS.toolList, JSON.stringify(Array.isArray(tools) ? tools : []));
    }

    window.McpApiCommon = {
        STORAGE_KEYS,
        initBaseFields,
        readBaseFields,
        buildUrl,
        requestJson,
        parseJsonSafe,
        logTo,
        updateStatus,
        updateSessionBox,
        parseSessionIdFromEndpoint,
        ensureSession,
        save,
        trimSlash,
        getStoredTools,
        setStoredTools,
        setInputValue
    };
})();
