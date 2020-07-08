/******************************************************
 * Copyright © x-tention Informationstechnologie GmbH
 * <a href="mailto:office@x-tention.at">office@x-tention.at</a> Dieses File ist Bestandteil des
 * Projektes elektronische Patientenakte AOK. Dieses File darf nicht kopiert und/oder verteilt
 * werden.
 *******************************************************/

package com.example.retry;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter responsible for logging outgoing requests and measuring response time.
 */
@Component
public class OutboundLogFilter implements GlobalFilter, Ordered {
  private Logger logger = LoggerFactory.getLogger(OutboundLogFilter.class);

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    URI requestUrl = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
    logger.info("Sending request to " + requestUrl);
    long startTime = System.currentTimeMillis();
    return chain.filter(exchange).doOnSuccess(success -> {
      logger.info("Received response from " + requestUrl);
      long endTime = System.currentTimeMillis();
      logger.info("Operation took {} ms", endTime - startTime);
    }).then();
  }
}
