package com.fullstyle.examfnbarroso;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamfnBarrosoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void getProductReturnsCatalogDetails() throws Exception {
		mockMvc.perform(get("/microservices/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Audifonos inalambricos"))
				.andExpect(jsonPath("$.priceUsd").value(79.99));
	}

	@Test
	void getUnknownProductReturnsNotFound() throws Exception {
		mockMvc.perform(get("/microservices/products/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getInventoryReturnsStockAndWarehouse() throws Exception {
		mockMvc.perform(get("/microservices/inventory/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(1))
				.andExpect(jsonPath("$.availableStock").value(25))
				.andExpect(jsonPath("$.warehouseLocation").value("Almacen central - Ciudad de Mexico"));
	}

	@Test
	void getUnknownInventoryReturnsNotFound() throws Exception {
		mockMvc.perform(get("/microservices/inventory/999"))
				.andExpect(status().isNotFound());
	}
}
