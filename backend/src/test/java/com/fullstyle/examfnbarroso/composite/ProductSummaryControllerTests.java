package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.fullstyle.examfnbarroso.currency.ExchangeRateResponse;
import com.fullstyle.examfnbarroso.currency.FrankfurterClient;

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

	@Test
	void getProductSummaryReturnsCombinedDetailsAndConvertedPrice() throws Exception {
		when(frankfurterClient.getUsdToMxn()).thenReturn(new ExchangeRateResponse(
				BigDecimal.ONE,
				"USD",
				LocalDate.of(2026, 10, 28),
				Map.of("MXN", new BigDecimal("19.85"))));

		mockMvc.perform(get("/api/v1/product-summary/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.product.id").value(1))
				.andExpect(jsonPath("$.product.priceUsd").value(79.99))
				.andExpect(jsonPath("$.inventory.availableStock").value(25))
				.andExpect(jsonPath("$.inventory.warehouseLocation").value("Almacen central - Ciudad de Mexico"))
				.andExpect(jsonPath("$.exchangeRate").value(19.85))
				.andExpect(jsonPath("$.exchangeRateDate").value("2026-10-28"))
				.andExpect(jsonPath("$.priceMxn").value(1587.80));
	}

	@Test
	void getProductSummaryReturnsNotFoundForUnknownProduct() throws Exception {
		when(frankfurterClient.getUsdToMxn()).thenReturn(new ExchangeRateResponse(
				BigDecimal.ONE,
				"USD",
				LocalDate.of(2026, 10, 28),
				Map.of("MXN", new BigDecimal("19.85"))));

		mockMvc.perform(get("/api/v1/product-summary/999"))
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
