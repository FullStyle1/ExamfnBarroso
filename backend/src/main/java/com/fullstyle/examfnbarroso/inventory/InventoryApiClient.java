package com.fullstyle.examfnbarroso.inventory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryApiClient {

	private final RestClient restClient;

	public InventoryApiClient(@Qualifier("inventoryRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public InventoryResponse getInventory(Long productId) {
		return restClient.get()
				.uri("/microservices/inventory/{productId}", productId)
				.retrieve()
				.body(InventoryResponse.class);
	}
}
