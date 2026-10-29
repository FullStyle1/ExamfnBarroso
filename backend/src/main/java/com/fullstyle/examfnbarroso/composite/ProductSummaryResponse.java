package com.fullstyle.examfnbarroso.composite;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fullstyle.examfnbarroso.catalog.ProductResponse;
import com.fullstyle.examfnbarroso.inventory.InventoryResponse;

public record ProductSummaryResponse(
		ProductResponse product,
		InventoryResponse inventory,
		BigDecimal exchangeRate,
		LocalDate exchangeRateDate,
		BigDecimal priceMxn) {
}
