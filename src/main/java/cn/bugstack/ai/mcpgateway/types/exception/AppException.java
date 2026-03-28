package cn.bugstack.ai.mcpgateway.types.exception;

public class AppException extends RuntimeException {

    private final String code;
    private final String info;

    public AppException(String code, String info) {
        super(info);
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
