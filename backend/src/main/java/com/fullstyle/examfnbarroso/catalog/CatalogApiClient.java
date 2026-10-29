package com.fullstyle.examfnbarroso.catalog;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogApiClient {

	private final RestClient restClient;

	public CatalogApiClient(@Qualifier("catalogRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public ProductResponse getProduct(Long id) {
		return restClient.get()
				.uri("/microservices/products/{id}", id)
				.retrieve()
				.body(ProductResponse.class);
	}
}
