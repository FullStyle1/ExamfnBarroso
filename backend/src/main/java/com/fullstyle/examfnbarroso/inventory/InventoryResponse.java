package com.fullstyle.examfnbarroso.inventory;

public record InventoryResponse(Long productId, int availableStock, String warehouseLocation) {
}
