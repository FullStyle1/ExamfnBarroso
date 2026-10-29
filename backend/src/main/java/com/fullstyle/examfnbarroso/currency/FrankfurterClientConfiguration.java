package com.fullstyle.examfnbarroso.currency;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FrankfurterClientConfiguration {

	@Bean
	RestClient frankfurterRestClient(
			RestClient.Builder builder,
			@Value("${frankfurter.base-url:https://api.frankfurter.app}") String baseUrl) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(5));

		return builder
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}
}
