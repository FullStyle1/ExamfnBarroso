package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import com.fullstyle.examfnbarroso.catalog.CatalogApiClient;
import com.fullstyle.examfnbarroso.catalog.ProductResponse;
import com.fullstyle.examfnbarroso.currency.ExchangeRateResponse;
import com.fullstyle.examfnbarroso.currency.FrankfurterClient;
import com.fullstyle.examfnbarroso.inventory.InventoryApiClient;
import com.fullstyle.examfnbarroso.inventory.InventoryResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductSummaryService {

	private static final String CURRENCY_SOURCE = "Frankfurter External API";
	private static final String SUCCESS_STATUS = "SUCCESS";

	private final CatalogApiClient catalogApiClient;
	private final InventoryApiClient inventoryApiClient;
	private final FrankfurterClient frankfurterClient;
	private final Executor executor;

	public ProductSummaryService(
			CatalogApiClient catalogApiClient,
			InventoryApiClient inventoryApiClient,
			FrankfurterClient frankfurterClient,
			@Qualifier("compositeExecutor") Executor executor) {
		this.catalogApiClient = catalogApiClient;
		this.inventoryApiClient = inventoryApiClient;
		this.frankfurterClient = frankfurterClient;
		this.executor = executor;
	}

	public ProductSummaryResponse getProductSummary(Long id) {
		CompletableFuture<ProductResponse> product = CompletableFuture.supplyAsync(
				() -> findProduct(id), executor);
		CompletableFuture<InventoryResponse> stock = CompletableFuture.supplyAsync(
				() -> findInventory(id), executor);
		CompletableFuture<ExchangeRateResponse> exchangeRate = CompletableFuture.supplyAsync(
				this::getExchangeRate, executor);

		try {
			CompletableFuture.allOf(product, stock, exchangeRate).join();
		} catch (CompletionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException("Product summary request failed", cause);
		}

		ProductResponse productDetails = product.join();
		InventoryResponse inventoryDetails = stock.join();
		ExchangeRateResponse rate = exchangeRate.join();
		BigDecimal priceMxn = productDetails.priceUsd()
				.multiply(rate.mxnRate())
				.setScale(2, RoundingMode.HALF_UP);

		return new ProductSummaryResponse(
				productDetails.id(),
				productDetails.name(),
				productDetails.description(),
				productDetails.category(),
				productDetails.priceUsd(),
				priceMxn,
				inventoryDetails.availableStock() > 0 ? "IN_STOCK" : "OUT_OF_STOCK",
				inventoryDetails.availableStock(),
				inventoryDetails.warehouseLocation(),
				inventoryDetails.estimatedDeliveryDays(),
				CURRENCY_SOURCE,
				rate.mxnRate(),
				rate.date(),
				SUCCESS_STATUS);
	}

	private ProductResponse findProduct(Long id) {
		try {
			return catalogApiClient.getProduct(id);
		} catch (RestClientResponseException exception) {
			throw mapInternalApiError(exception, "Catalog");
		} catch (RestClientException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Catalog service is unavailable", exception);
		}
	}

	private InventoryResponse findInventory(Long id) {
		try {
			return inventoryApiClient.getInventory(id);
		} catch (RestClientResponseException exception) {
			throw mapInternalApiError(exception, "Inventory");
		} catch (RestClientException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory service is unavailable", exception);
		}
	}

	private ExchangeRateResponse getExchangeRate() {
		try {
			return frankfurterClient.getUsdToMxn();
		} catch (RestClientException | IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Exchange rate service is unavailable", exception);
		}
	}

	private ResponseStatusException mapInternalApiError(RestClientResponseException exception, String serviceName) {
		if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
			return new ResponseStatusException(HttpStatus.NOT_FOUND, serviceName + " resource not found", exception);
		}
		return new ResponseStatusException(
				HttpStatus.BAD_GATEWAY, serviceName + " service returned an error", exception);
	}
}
