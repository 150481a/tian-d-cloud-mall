package com.td.boot.starter.log.filter;

import com.td.boot.starter.log.properties.LogProperties;
import com.td.boot.starter.log.utils.TraceIdGenerator;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * TraceId 過濾器。
 * 在 HTTP 請求進入時生成或獲取 TraceId 並放入 MDC，請求結束時清除 MDC。
 * 確保每個請求的日誌都能被追踪。
 */
@Slf4j
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_KEY = "traceId"; // MDC 中 TraceId 的 Key
    public static final String SPAN_ID_KEY = "spanId";   // MDC 中 SpanId 的 Key (可選)

    private final LogProperties logProperties;

    public TraceIdFilter(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 1. 獲取或生成 TraceId
            String traceId = httpRequest.getHeader(logProperties.getRequestIdHeaderName());
            traceId = TraceIdGenerator.getOrCreateTraceId(traceId);
            MDC.put(TRACE_ID_KEY, traceId);

            // 2. 生成 SpanId (可選，如果需要更詳細的追踪)
            String spanId = TraceIdGenerator.generateTraceId(); // 簡單生成一個新的
            MDC.put(SPAN_ID_KEY, spanId);

            chain.doFilter(request, response);
        } finally {
            // 3. 請求處理完成後，清除 MDC 中的 TraceId 和 SpanId
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }
}
