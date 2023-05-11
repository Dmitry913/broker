package org.wasend.broker.controller.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
@Slf4j
public class LogFilter extends OncePerRequestFilter {

    // TODO можно сделать иначе, чтобы логировать и данные с body
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String logInfo =
                "\nPath: " + request.getRequestURI() + "\n" +
                "Method: " + request.getMethod() + "\n" +
                "Headers: " + getAllHeaders(request);
        log.info(logInfo);
        filterChain.doFilter(request, response);
    }

    private List<String> getAllHeaders(HttpServletRequest request) {
        List<String> headersValue = new ArrayList<>();
        Enumeration<String> headerName = request.getHeaderNames();
        while(headerName.hasMoreElements()) {
            headersValue.add(headerName.nextElement());
        }
        return headersValue;
    }
}
