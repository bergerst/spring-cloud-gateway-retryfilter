/******************************************************
 * Copyright © x-tention Informationstechnologie GmbH
 * <a href="mailto:office@x-tention.at">office@x-tention.at</a> Dieses File ist Bestandteil des
 * Projektes elektronische Patientenakte AOK. Dieses File darf nicht kopiert und/oder verteilt
 * werden.
 *******************************************************/

package com.example.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.example.retry.RetryFilterTest.RouteConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 64324, httpsPort = 0)
@Import(RouteConfiguration.class)
public class RetryFilterTest {

  @TestConfiguration
  static class RouteConfiguration {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
      RetryGatewayFilterFactory factory = new RetryGatewayFilterFactory();
      OrderedGatewayFilter filter = new OrderedGatewayFilter(factory.apply(new RetryConfig()), 1);

      return builder.routes()
          .route(spec -> spec.path("/test").and().method(HttpMethod.GET)
              .filters(f -> f.filter(filter)).uri("http://localhost:64324").id("retry-test"))
          .build();
    }
  }

  @Autowired
  private WebTestClient webClient;

  @Test
  public void testRetry() {
    stubFor(get(urlEqualTo("/test")).willReturn(aResponse().withStatus(503)));

    webClient.get().uri("/test").exchange();

    verify(4, getRequestedFor(urlEqualTo("/test")));
  }

}
