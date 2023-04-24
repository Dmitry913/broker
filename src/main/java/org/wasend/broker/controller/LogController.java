package org.wasend.broker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
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
public class LogController extends OncePerRequestFilter {

    // TODO можно сделать иначе, чтобы логировать и данные с body
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String logInfo =
                "Path: " + request.getContextPath() + "\n" +
                "Method: " + request.getMethod() + "\n" +
                "Headers: " + getAllHeaders(request) + "\n";
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
