package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.fullstyle.examfnbarroso.currency.ExchangeRateResponse;
import com.fullstyle.examfnbarroso.currency.FrankfurterClient;
import com.fullstyle.examfnbarroso.catalog.ProductResponse;
import com.fullstyle.examfnbarroso.catalog.CatalogApiClient;
import com.fullstyle.examfnbarroso.inventory.InventoryApiClient;
import com.fullstyle.examfnbarroso.inventory.InventoryResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSummaryControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FrankfurterClient frankfurterClient;

	@MockitoBean
	private CatalogApiClient catalogApiClient;

	@MockitoBean
	private InventoryApiClient inventoryApiClient;

	@Test
	void getProductSummaryReturnsCombinedDetailsAndConvertedPrice() throws Exception {
		stubProductSummarySources(1L);

		mockMvc.perform(get("/api/v1/product-summary/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.product_id").value(1))
				.andExpect(jsonPath("$.title").value("Audifonos inalambricos"))
				.andExpect(jsonPath("$.category").value("Electronica"))
				.andExpect(jsonPath("$.price_usd").value(79.99))
				.andExpect(jsonPath("$.price_mxn").value(1587.80))
				.andExpect(jsonPath("$.stock_status").value("IN_STOCK"))
				.andExpect(jsonPath("$.available_units").value(25))
				.andExpect(jsonPath("$.warehouse_location").value("Almacen central - Ciudad de Mexico"))
				.andExpect(jsonPath("$.estimated_delivery_days").value(2))
				.andExpect(jsonPath("$.currency_conversion_source").value("Frankfurter External API"))
				.andExpect(jsonPath("$.exchange_rate").value(19.85))
				.andExpect(jsonPath("$.exchange_rate_date").value("2026-10-28"))
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	@Test
	void getAssignmentCompositeRouteReturnsSameContract() throws Exception {
		stubProductSummarySources(1L);

		mockMvc.perform(get("/api/v1/products/1/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.product_id").value(1))
				.andExpect(jsonPath("$.price_mxn").value(1587.80))
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	private void stubProductSummarySources(Long id) {
		stubProductSummarySources(id, 25);
	}

	private void stubProductSummarySources(Long id, int availableStock) {
		when(catalogApiClient.getProduct(id)).thenReturn(new ProductResponse(
				id,
				"Audifonos inalambricos",
				"Audifonos Bluetooth con cancelacion de ruido.",
				"Electronica",
				new BigDecimal("79.99")));
		when(inventoryApiClient.getInventory(id)).thenReturn(new InventoryResponse(
				id, availableStock, "Almacen central - Ciudad de Mexico", 2));
		when(frankfurterClient.getUsdToMxn()).thenReturn(new ExchangeRateResponse(
				BigDecimal.ONE,
				"USD",
				LocalDate.of(2026, 10, 28),
				Map.of("MXN", new BigDecimal("19.85"))));
	}

	@Test
	void getProductSummaryMarksProductOutOfStock() throws Exception {
		stubProductSummarySources(1L, 0);

		mockMvc.perform(get("/api/v1/product-summary/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock_status").value("OUT_OF_STOCK"))
				.andExpect(jsonPath("$.available_units").value(0));
	}

	@Test
	void getProductSummaryReturnsNotFoundForUnknownProduct() throws Exception {
		when(catalogApiClient.getProduct(999L)).thenThrow(
				org.springframework.web.client.HttpClientErrorException.create(
						HttpStatus.NOT_FOUND, "Not Found", org.springframework.http.HttpHeaders.EMPTY,
						new byte[0], null));
		when(frankfurterClient.getUsdToMxn()).thenReturn(new ExchangeRateResponse(
				BigDecimal.ONE,
				"USD",
				LocalDate.of(2026, 10, 28),
				Map.of("MXN", new BigDecimal("19.85"))));

		mockMvc.perform(get("/api/v1/product-summary/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getProductSummaryReturnsNotFoundWhenInventoryIsMissing() throws Exception {
		when(catalogApiClient.getProduct(1L)).thenReturn(new ProductResponse(
				1L, "Audifonos inalambricos", "Audifonos Bluetooth con cancelacion de ruido.",
				"Electronica", new BigDecimal("79.99")));
		when(inventoryApiClient.getInventory(1L)).thenThrow(
				org.springframework.web.client.HttpClientErrorException.create(
						HttpStatus.NOT_FOUND, "Not Found", org.springframework.http.HttpHeaders.EMPTY,
						new byte[0], null));
		when(frankfurterClient.getUsdToMxn()).thenReturn(new ExchangeRateResponse(
				BigDecimal.ONE, "USD", LocalDate.of(2026, 10, 28), Map.of("MXN", new BigDecimal("19.85"))));

		mockMvc.perform(get("/api/v1/product-summary/1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getProductSummaryReturnsBadGatewayWhenCurrencyServiceFails() throws Exception {
		when(frankfurterClient.getUsdToMxn())
				.thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Frankfurter unavailable"));

		mockMvc.perform(get("/api/v1/product-summary/1"))
				.andExpect(status().isBadGateway());
	}
}
