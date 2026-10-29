package com.fullstyle.examfnbarroso;

import java.math.BigDecimal;

import com.fullstyle.examfnbarroso.catalog.CatalogApiClient;
import com.fullstyle.examfnbarroso.inventory.InventoryApiClient;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InternalApiClientTests {

	@Test
	void catalogClientCallsProductRestEndpoint() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog-service");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		CatalogApiClient client = new CatalogApiClient(builder.build());

		server.expect(requestTo("http://catalog-service/microservices/products/1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "id": 1,
						  "name": "Audifonos inalambricos",
						  "description": "Audifonos Bluetooth con cancelacion de ruido.",
						  "category": "Electronica",
						  "priceUsd": 79.99
						}
						""", MediaType.APPLICATION_JSON));

		var product = client.getProduct(1L);

		assertEquals("Electronica", product.category());
		assertEquals(new BigDecimal("79.99"), product.priceUsd());
		server.verify();
	}

	@Test
	void inventoryClientCallsInventoryRestEndpoint() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory-service");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		InventoryApiClient client = new InventoryApiClient(builder.build());

		server.expect(requestTo("http://inventory-service/microservices/inventory/1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "productId": 1,
						  "availableStock": 25,
						  "warehouseLocation": "Almacen central - Ciudad de Mexico",
						  "estimatedDeliveryDays": 2
						}
						""", MediaType.APPLICATION_JSON));

		var inventory = client.getInventory(1L);

		assertEquals(25, inventory.availableStock());
		assertEquals(2, inventory.estimatedDeliveryDays());
		server.verify();
	}
}
