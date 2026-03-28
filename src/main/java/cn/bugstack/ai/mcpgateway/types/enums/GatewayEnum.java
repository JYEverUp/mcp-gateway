package cn.bugstack.ai.mcpgateway.types.enums;

import cn.bugstack.ai.mcpgateway.types.exception.AppException;

public enum GatewayEnum {

    ;

    public enum GatewayStatus {
        NOT_VERIFIED(0, "不校验"),
        STRONG_VERIFIED(1, "强校验");

        private final Integer code;
        private final String info;

        GatewayStatus(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        public Integer getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }

        public static GatewayStatus get(Integer code) {
            if (code == null) {
                return null;
            }
            for (GatewayStatus value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            throw new AppException(ResponseCode.ENUM_NOT_FOUND.getCode(), ResponseCode.ENUM_NOT_FOUND.getInfo());
        }
    }

    public enum GatewayAuthStatusEnum {
        ENABLE(1, "启用"),
        DISABLE(0, "禁用");

        private final Integer code;
        private final String info;

        GatewayAuthStatusEnum(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        public Integer getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }

        public static GatewayAuthStatusEnum getByCode(Integer code) {
            if (code == null) {
                return null;
            }
            for (GatewayAuthStatusEnum value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            throw new AppException(ResponseCode.ENUM_NOT_FOUND.getCode(), ResponseCode.ENUM_NOT_FOUND.getInfo());
        }
    }

}
