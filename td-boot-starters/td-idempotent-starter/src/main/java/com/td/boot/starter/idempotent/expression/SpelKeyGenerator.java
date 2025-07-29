package com.td.boot.starter.idempotent.expression;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * SpEL 表達式 Key 生成器。
 * 用於解析方法參數、請求屬性等生成冪等 Key。
 */
@Component
public class SpelKeyGenerator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 解析 SpEL 表達式生成 Key。
     *
     * @param keyExpression SpEL 表達式，例如 "#arg[0].id" 或 "#request.getHeader('X-Request-Id')"
     * @param context       已準備好的 SpEL 評估上下文，包含方法參數和請求對象等變量。
     * @return 解析後的 Key 字符串
     */
    public String generateKey(String keyExpression, StandardEvaluationContext context) {
        try {
            return parser.parseExpression(keyExpression).getValue(context, String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("SpEL 表達式解析失敗: " + keyExpression, e);
        }
    }

}
