package com.majortom.algorithms.core.runtime;

/**
 * 算法执行异常。
 *
 * <p>它比普通 {@link RuntimeException} 多一个稳定 code，控制器或 UI 层可以用 code
 * 做国际化、分类展示或忽略用户主动取消这类正常中断。</p>
 */
public class ExecutionException extends RuntimeException {

    /**
     * 稳定错误编码。
     */
    private final String code;

    /**
     * 创建执行异常。
     *
     * @param code 稳定错误编码
     * @param message 错误消息
     */
    public ExecutionException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建带原始异常的执行异常。
     *
     * @param code 稳定错误编码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ExecutionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取稳定错误编码。
     *
     * @return 错误编码
     */
    public String getCode() {
        return code;
    }
}
