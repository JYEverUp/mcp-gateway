package cn.bugstack.ai.mcpgateway.domain.session.model.valobj.enums;

public enum SessionMessageHandlerMethodEnum {

    INITIALIZE("initialize", "initializeHandler"),
    TOOLS_LIST("tools/list", "toolsListHandler"),
    TOOLS_CALL("tools/call", "toolsCallHandler"),
    RESOURCES_LIST("resources/list", "resourcesListHandler");

    private final String method;
    private final String handlerName;

    SessionMessageHandlerMethodEnum(String method, String handlerName) {
        this.method = method;
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public static SessionMessageHandlerMethodEnum getByMethod(String method) {
        for (SessionMessageHandlerMethodEnum value : values()) {
            if (value.method.equals(method)) {
                return value;
            }
        }
        return null;
    }

}
