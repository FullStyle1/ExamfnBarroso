package com.fullstyle.examfnbarroso.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ExchangeRateResponse(BigDecimal amount, String base, LocalDate date, Map<String, BigDecimal> rates) {

	public BigDecimal mxnRate() {
		BigDecimal rate = rates == null ? null : rates.get("MXN");
		if (rate == null) {
			throw new IllegalStateException("Frankfurter response does not contain the USD to MXN rate");
		}
		return rate;
	}
}
