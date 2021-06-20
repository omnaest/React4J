package org.omnaest.react4j.service.internal.controller.utils;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class RequestProvider implements Filter
{
    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest)
        {
            this.request.set((HttpServletRequest) request);
        }
        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            this.request.set(null);
        }
    }

    public Optional<HttpServletRequest> getRequest()
    {
        return Optional.ofNullable(this.request.get());
    }

    public Optional<String> getRequestBaseUrl()
    {
        return this.getRequest()
                   .map(request -> ServletUriComponentsBuilder.fromRequestUri(request)
                                                              .replacePath(null)
                                                              .build()
                                                              .toUriString());
    }
}
