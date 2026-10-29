package com.fullstyle.examfnbarroso.composite;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class InternalApiClientConfiguration {

	@Bean("catalogRestClient")
	RestClient catalogRestClient(
			RestClient.Builder builder,
			@Value("${services.catalog.base-url:http://localhost:8080}") String baseUrl) {
		return createClient(builder, baseUrl);
	}

	@Bean("inventoryRestClient")
	RestClient inventoryRestClient(
			RestClient.Builder builder,
			@Value("${services.inventory.base-url:http://localhost:8080}") String baseUrl) {
		return createClient(builder, baseUrl);
	}

	private RestClient createClient(RestClient.Builder builder, String baseUrl) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(5));

		return builder.clone()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}
}
