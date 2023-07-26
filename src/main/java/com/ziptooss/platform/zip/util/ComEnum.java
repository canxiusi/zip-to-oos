package com.ziptooss.platform.zip.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yukun.yan
 * @description ComEnum
 * @date 2023/7/24 13:19
 */
public class ComEnum {

    @Getter
    @AllArgsConstructor
    public enum RspEnum {
        /**/
        success(200, "执行成功"),
        error(500, "执行错误"),

        ;
        private final Integer code;
        private final String msg;

    }

}
