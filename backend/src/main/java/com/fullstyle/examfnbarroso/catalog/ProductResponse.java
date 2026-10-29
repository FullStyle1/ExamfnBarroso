package com.fullstyle.examfnbarroso.catalog;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal priceUsd) {
}
