package cn.bugstack.ai.mcpgateway.api.response;

public class Response<T> {

    private final String code;
    private final String info;
    private final T data;

    private Response(String code, String info, T data) {
        this.code = code;
        this.info = info;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>("0000", "成功", data);
    }

    public static <T> Response<T> error(String code, String info) {
        return new Response<>(code, info, null);
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public T getData() {
        return data;
    }

}
