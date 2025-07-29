package com.td.boot.starter.log.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 請求體緩存過濾器。
 * 目的：允許 HTTP 請求體（InputStream）被多次讀取。
 * Spring Framework 提供了 ContentCachingRequestWrapper 來實現這個功能。
 * 此過濾器將 HttpServletRequest 包裝成 ContentCachingRequestWrapper。
 */
@Slf4j
public class RequestBodyCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 判斷是否需要緩存請求體 (例如，只緩存 POST/PUT 請求)
        // 為了通用性，這裡對所有請求都進行緩存，但 ContentCachingRequestWrapper 會智能判斷是否緩存。
        // 如果請求方法不是 POST/PUT 或 Content-Type 不包含應用層數據，它不會真的緩存。
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        log.trace("RequestBodyCachingFilter 包裝請求: {}", httpRequest.getRequestURI());
        chain.doFilter(requestWrapper, response);
    }
}
