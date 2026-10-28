package com.atelier.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-signing-key-that-is-at-least-32-bytes-long",
        "app.admin.email=admin@atelier.local",
        "app.admin.password=test-only-admin-password"
})
@AutoConfigureMockMvc
class StoreApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void exposesSeededProductsPublicly() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("size", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(8)))
                .andExpect(jsonPath("$.content[0].name").value("Forma Ceramic Vase"));
    }

    @Test
    void filtersProductsByPriceAndRejectsAnInvalidPriceRange() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("size", "24")
                        .param("minPrice", "50").param("maxPrice", "80")
                        .param("sortBy", "price").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(52.00))
                .andExpect(jsonPath("$.content[1].price").value(68.00));
        mvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "50"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("search", "Forma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Forma Ceramic Vase"));
    }

    @Test
    void rejectsUnauthenticatedAdminProductChanges() throws Exception {
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Test object", "category", "Home & Living", "description", "Test",
                                "price", 22.00, "stock", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatesCustomerAndPersistsOrderUsingServerPrices() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Test Customer", "email", email, "password", "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(registration).path("token").asText();

        String productResponse = mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode product = mapper.readTree(productResponse).path("content").get(0);
        long productId = product.path("id").asLong();
        String price = product.path("price").decimalValue().toPlainString();

        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "address", "12 Market Street, Portland, OR 97201",
                                "items", new Object[]{Map.of("productId", productId, "quantity", 1)},
                                "total", 0.01))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(Double.parseDouble(price)))
                .andExpect(jsonPath("$.items[0].productName").value("Forma Ceramic Vase"));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].total").value(Double.parseDouble(price)));
    }

    @Test
    void persistsCartAndCheckoutClearsItAfterCreatingServerPricedOrder() throws Exception {
        String email = "cart-" + UUID.randomUUID() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Cart Customer", "email", email, "password", "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(registration).path("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andReturn().getResponse().getContentAsString()).path("content").get(0);
        long productId = product.path("id").asLong();
        double unitPrice = product.path("price").asDouble();

        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(unitPrice * 2));

        mvc.perform(post("/api/orders/checkout").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("address", "12 Market Street, Portland, OR 97201"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(unitPrice * 2))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.subtotal").value(0));
    }

    @Test
    void checkoutRejectsCombinedDuplicateLinesThatExceedStockWithoutChangingInventory() throws Exception {
        String email = "stock-" + UUID.randomUUID() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Stock Customer", "email", email, "password", "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(registration).path("token").asText();
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andReturn().getResponse().getContentAsString()).path("content").get(0);
        long productId = product.path("id").asLong();
        int originalStock = product.path("stock").asInt();

        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "address", "12 Market Street, Portland, OR 97201",
                                "items", new Object[]{Map.of("productId", productId, "quantity", originalStock),
                                        Map.of("productId", productId, "quantity", 1)}))))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(originalStock));
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void categoryManagementAndDashboardAreRestrictedToAdministrators() throws Exception {
        String customerEmail = "dashboard-" + UUID.randomUUID() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Dashboard Customer", "email", customerEmail, "password", "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userToken = mapper.readTree(registration).path("token").asText();

        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        JsonNode listedCategories = mapper.readTree(mvc.perform(get("/api/categories"))
                .andReturn().getResponse().getContentAsString());
        long assignedCategoryId = -1;
        for (JsonNode category : listedCategories) {
            if ("Home & Living".equals(category.path("name").asText())) assignedCategoryId = category.path("id").asLong();
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/categories/" + assignedCategoryId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminLogin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "admin@atelier.local", "password", "test-only-admin-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = mapper.readTree(adminLogin).path("token").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/categories/" + assignedCategoryId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").value(8))
                .andExpect(jsonPath("$.inventoryUnits").isNumber());

        String categoryJson = mapper.writeValueAsString(Map.of("name", "Seasonal Test", "description", "Temporary category"));
        String newCategory = mvc.perform(post("/api/admin/categories").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Seasonal Test"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/admin/categories").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson))
                .andExpect(status().isConflict());
        long newCategoryId = mapper.readTree(newCategory).path("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/categories/" + newCategoryId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersCannotReadAnotherCustomersOrder() throws Exception {
        String firstToken = registerAndGetToken("order-owner");
        String secondToken = registerAndGetToken("other-customer");
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andReturn().getResponse().getContentAsString()).path("content").get(0);
        String createdOrder = mvc.perform(post("/api/orders").header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("address", "12 Market Street",
                                "items", List.of(Map.of("productId", product.path("id").asLong(), "quantity", 1))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(createdOrder).path("id").asLong();

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderStatusTransitionsAreValidatedAndCancellationRestoresStock() throws Exception {
        String customerToken = registerAndGetToken("status-owner");
        String adminLogin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "admin@atelier.local", "password", "test-only-admin-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = mapper.readTree(adminLogin).path("token").asText();
        JsonNode product = mapper.readTree(mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andReturn().getResponse().getContentAsString()).path("content").get(0);
        long productId = product.path("id").asLong();
        int stockBeforeOrder = product.path("stock").asInt();
        String createdOrder = mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("address", "12 Market Street",
                                "items", List.of(Map.of("productId", productId, "quantity", 1))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(createdOrder).path("id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/admin/orders/" + orderId + "/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("status", "delivered"))))
                .andExpect(status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/admin/orders/" + orderId + "/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("status", "cancelled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
        mvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(stockBeforeOrder));
    }

    private String registerAndGetToken(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Test Customer", "email", email, "password", "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(registration).path("token").asText();
    }
}
