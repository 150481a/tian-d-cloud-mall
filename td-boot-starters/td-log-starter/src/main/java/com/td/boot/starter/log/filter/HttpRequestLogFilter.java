package com.td.boot.starter.log.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.td.boot.starter.log.properties.LogProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 請求響應日誌過濾器。
 * 記錄請求 URL、方法、參數、頭信息、響應狀態、耗時等。
 */
@Slf4j
public class HttpRequestLogFilter implements Filter {
    private final LogProperties logProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper(); // 用於 JSON 轉換

    public HttpRequestLogFilter(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // 排除指定 URL
        String requestUri = httpRequest.getRequestURI();
        if (!CollectionUtils.isEmpty(logProperties.getHttpRequestLogExcludes())) {
            for (String excludePattern : logProperties.getHttpRequestLogExcludes()) {
                if (pathMatcher.match(excludePattern, requestUri)) {
                    log.trace("排除日誌記錄的 URL: {}", requestUri);
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        try {
            // 將請求和響應包裝，以便在鏈的末尾讀取內容
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 記錄請求日誌
            logRequest(requestWrapper, duration);

            // 記錄響應日誌
            logResponse(requestWrapper, responseWrapper, duration);

            // 必須執行此步驟，否則響應體將不會被寫入到客戶端
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper requestWrapper, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- HTTP Request Log ---\n");
        sb.append("URI: ").append(requestWrapper.getMethod()).append(" ").append(requestWrapper.getRequestURI());
        if (StringUtils.hasText(requestWrapper.getQueryString())) {
            sb.append("?").append(requestWrapper.getQueryString());
        }
        sb.append("\nHeaders: ").append(getHeaders(requestWrapper));
        sb.append("\nClient IP: ").append(getClientIp(requestWrapper));

        if (logProperties.isLogRequestBody() && requestWrapper.getContentAsByteArray().length > 0) {
            String requestBody = getContentAsString(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
            sb.append("\nRequest Body: ").append(requestBody);
        }
        sb.append("\n------------------------");
        log.info(sb.toString());
    }

    private void logResponse(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- HTTP Response Log ---\n");
        sb.append("URI: ").append(requestWrapper.getMethod()).append(" ").append(requestWrapper.getRequestURI());
        sb.append("\nStatus: ").append(responseWrapper.getStatus());
        sb.append(" (").append(HttpStatus.valueOf(responseWrapper.getStatus()).getReasonPhrase()).append(")");
        sb.append("\nDuration: ").append(duration).append(" ms");

        if (logProperties.isLogResponseBody() && responseWrapper.getContentAsByteArray().length > 0) {
            String responseBody = getContentAsString(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
            sb.append("\nResponse Body: ").append(responseBody);
        }
        sb.append("\n-------------------------");
        log.info(sb.toString());
    }

    private String getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            log.error("轉換 Headers 為 JSON 失敗", e);
            return headers.toString();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getContentAsString(byte[] buf, String charsetName) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        try {
            return new String(buf, charsetName);
        } catch (UnsupportedEncodingException ex) {
            return new String(buf);
        }
    }
}
