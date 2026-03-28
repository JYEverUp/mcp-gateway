package cn.bugstack.ai.mcpgateway.types.enums;

public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    METHOD_NOT_FOUND("0003", "未找到方法"),
    ENUM_NOT_FOUND("0004", "枚举不存在"),
    DB_UPDATE_FAIL("0005", "数据库更新失败"),
    AUTH_FAIL("0006", "鉴权失败"),
    RATE_LIMIT_FAIL("0007", "触发限流");

    private final String code;
    private final String info;

    ResponseCode(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

}
