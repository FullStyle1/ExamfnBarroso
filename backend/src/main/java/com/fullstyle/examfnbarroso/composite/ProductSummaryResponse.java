package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductSummaryResponse(
		@JsonProperty("product_id") Long productId,
		String title,
		String description,
		String category,
		@JsonProperty("price_usd") BigDecimal priceUsd,
		@JsonProperty("price_mxn") BigDecimal priceMxn,
		@JsonProperty("stock_status") String stockStatus,
		@JsonProperty("available_units") int availableUnits,
		@JsonProperty("warehouse_location") String warehouseLocation,
		@JsonProperty("estimated_delivery_days") int estimatedDeliveryDays,
		@JsonProperty("currency_conversion_source") String currencyConversionSource,
		@JsonProperty("exchange_rate") BigDecimal exchangeRate,
		@JsonProperty("exchange_rate_date") LocalDate exchangeRateDate,
		String status) {
}
