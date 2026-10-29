package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import com.fullstyle.examfnbarroso.catalog.Product;
import com.fullstyle.examfnbarroso.catalog.ProductRepository;
import com.fullstyle.examfnbarroso.catalog.ProductResponse;
import com.fullstyle.examfnbarroso.currency.ExchangeRateResponse;
import com.fullstyle.examfnbarroso.currency.FrankfurterClient;
import com.fullstyle.examfnbarroso.inventory.Inventory;
import com.fullstyle.examfnbarroso.inventory.InventoryRepository;
import com.fullstyle.examfnbarroso.inventory.InventoryResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductSummaryService {

	private final ProductRepository products;
	private final InventoryRepository inventory;
	private final FrankfurterClient frankfurterClient;
	private final Executor executor;

	public ProductSummaryService(
			ProductRepository products,
			InventoryRepository inventory,
			FrankfurterClient frankfurterClient,
			@Qualifier("compositeExecutor") Executor executor) {
		this.products = products;
		this.inventory = inventory;
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
				productDetails,
				inventoryDetails,
				rate.mxnRate(),
				rate.date(),
				priceMxn);
	}

	private ProductResponse findProduct(Long id) {
		Product product = products.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
		return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPriceUsd());
	}

	private InventoryResponse findInventory(Long id) {
		Inventory item = inventory.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
		return new InventoryResponse(item.getProductId(), item.getAvailableStock(), item.getWarehouseLocation());
	}

	private ExchangeRateResponse getExchangeRate() {
		try {
			return frankfurterClient.getUsdToMxn();
		} catch (RestClientException | IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Exchange rate service is unavailable", exception);
		}
	}
}
