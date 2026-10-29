package com.fullstyle.examfnbarroso.currency;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterClientTests {

	private MockRestServiceServer server;
	private FrankfurterClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.frankfurter.app");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new FrankfurterClient(builder.build());
	}

	@AfterEach
	void verifyRequests() {
		server.verify();
	}

	@Test
	void getUsdToMxnReturnsRateDateAndBase() {
		server.expect(requestTo("https://api.frankfurter.app/latest?from=USD&to=MXN"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "amount": 1.0,
						  "base": "USD",
						  "date": "2026-10-28",
						  "rates": { "MXN": 19.85 }
						}
						""", MediaType.APPLICATION_JSON));

		ExchangeRateResponse response = client.getUsdToMxn();

		assertEquals("USD", response.base());
		assertEquals(LocalDate.of(2026, 10, 28), response.date());
		assertEquals(new BigDecimal("19.85"), response.mxnRate());
	}

	@Test
	void getUsdToMxnRejectsResponseWithoutRate() {
		server.expect(requestTo("https://api.frankfurter.app/latest?from=USD&to=MXN"))
				.andRespond(withSuccess("""
						{
						  "amount": 1.0,
						  "base": "USD",
						  "date": "2026-10-28",
						  "rates": {}
						}
						""", MediaType.APPLICATION_JSON));

		assertThrows(IllegalStateException.class, client::getUsdToMxn);
	}

	@Test
	void getUsdToMxnPropagatesExternalServiceErrors() {
		server.expect(requestTo("https://api.frankfurter.app/latest?from=USD&to=MXN"))
				.andRespond(withServerError());

		assertThrows(RestClientResponseException.class, client::getUsdToMxn);
	}

	@Test
	void configuredClientFollowsFrankfurterRedirects() throws IOException {
		AtomicInteger requestCount = new AtomicInteger();
		HttpServer redirectServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		redirectServer.createContext("/", exchange -> {
			requestCount.incrementAndGet();
			if ("/latest".equals(exchange.getRequestURI().getPath())) {
				exchange.getResponseHeaders().add("Location", "/v1/latest?from=USD&to=MXN");
				exchange.sendResponseHeaders(301, -1);
			} else {
				byte[] response = """
						{
						  "amount": 1.0,
						  "base": "USD",
						  "date": "2026-10-28",
						  "rates": { "MXN": 19.85 }
						}
						""".getBytes(java.nio.charset.StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, response.length);
				exchange.getResponseBody().write(response);
			}
			exchange.close();
		});
		redirectServer.start();

		try {
			String baseUrl = "http://localhost:" + redirectServer.getAddress().getPort();
			RestClient redirectingRestClient = new FrankfurterClientConfiguration()
					.frankfurterRestClient(RestClient.builder(), baseUrl);
			FrankfurterClient redirectingClient = new FrankfurterClient(redirectingRestClient);

			assertEquals(new BigDecimal("19.85"), redirectingClient.getUsdToMxn().mxnRate());
			assertEquals(2, requestCount.get());
		} finally {
			redirectServer.stop(0);
		}
	}
}
