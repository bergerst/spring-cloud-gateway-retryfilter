package com.example.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.example.retry.RetryFilterTest.RouteConfiguration;
import org.apache.http.HttpHeaders;
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
      OrderedGatewayFilter retryFilter =
          new OrderedGatewayFilter(factory.apply(new RetryConfig().setRetries(1)), 1);

      return builder.routes()
          .route(spec -> spec.path("/test").and().method(HttpMethod.GET)
              .filters(f -> f.filters(retryFilter)).uri("http://localhost:64324").id("retry-test"))
          .build();
    }
  }

  @Autowired
  private WebTestClient webClient;

  @Test
  public void testRetry() throws InterruptedException {
    stubFor(get(urlEqualTo("/test")).willReturn(aResponse().withStatus(503)));

    for (int i = 0; i < 5; ++i) {
      var response =
          webClient.get().uri("/test").header(HttpHeaders.CONTENT_TYPE, "text/xml").exchange();
      response.expectStatus().isEqualTo(503);
    }
  }
}
