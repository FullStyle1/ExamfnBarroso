package com.fullstyle.examfnbarroso.currency;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FrankfurterClient {

	private final RestClient restClient;

	public FrankfurterClient(RestClient frankfurterRestClient) {
		this.restClient = frankfurterRestClient;
	}

	public ExchangeRateResponse getUsdToMxn() {
		ExchangeRateResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder.path("/latest")
						.queryParam("from", "USD")
						.queryParam("to", "MXN")
						.build())
				.retrieve()
				.body(ExchangeRateResponse.class);

		if (response == null || !"USD".equals(response.base())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from Frankfurter");
		}
		response.mxnRate();
		return response;
	}
}
