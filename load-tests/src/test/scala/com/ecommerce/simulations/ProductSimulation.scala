package com.ecommerce.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ProductSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  // Feeder for product data
  val productFeeder = csv("data/products.csv").circular
  val categoryFeeder = csv("data/categories.csv").circular

  // Scenario: Browse Products
  val browseProducts = scenario("Browse Products")
    .exec(
      http("Get All Products")
        .get("/api/products")
        .queryParam("page", "0")
        .queryParam("size", "20")
        .check(status.is(200))
        .check(jsonPath("$[*].id").findAll.saveAs("productIds"))
    )
    .pause(1, 3)
    .exec(
      http("Get Product by ID")
        .get("/api/products/${productIds.random()}")
        .check(status.in(200, 404))
    )
    .pause(1, 2)

  // Scenario: Search Products
  val searchProducts = scenario("Search Products")
    .feed(csv("data/search-terms.csv").circular)
    .exec(
      http("Search Products")
        .get("/api/products/search")
        .queryParam("q", "${searchTerm}")
        .check(status.is(200))
    )
    .pause(1, 2)
    .exec(
      http("Search with Filters")
        .get("/api/products")
        .queryParam("search", "${searchTerm}")
        .queryParam("page", "0")
        .queryParam("size", "10")
        .check(status.is(200))
    )
    .pause(1, 2)

  // Scenario: Product CRUD Operations
  val productCRUD = scenario("Product CRUD Operations")
    .feed(productFeeder)
    .exec(
      http("Create Product")
        .post("/api/products")
        .body(StringBody("""{
          "name": "${productName}",
          "description": "${productDescription}",
          "price": ${productPrice},
          "categoryId": ${categoryId},
          "stock": ${stock}
        }""")).asJson
        .check(status.in(201, 400))
        .check(jsonPath("$.id").optional.saveAs("createdProductId"))
    )
    .pause(1, 2)
    .doIf("${createdProductId.exists()}") {
      exec(
        http("Get Created Product")
          .get("/api/products/${createdProductId}")
          .check(status.is(200))
      )
      .pause(1, 2)
      .exec(
        http("Update Product")
          .put("/api/products/${createdProductId}")
          .body(StringBody("""{
            "name": "${productName} - Updated",
            "description": "${productDescription}",
            "price": ${productPrice},
            "categoryId": ${categoryId},
            "stock": ${stock}
          }""")).asJson
          .check(status.in(200, 404))
      )
      .pause(1, 2)
    }

  // Scenario: Products by Category
  val productsByCategory = scenario("Products by Category")
    .feed(categoryFeeder)
    .exec(
      http("Get Products by Category")
        .get("/api/products/category/${categoryId}")
        .check(status.in(200, 404))
    )
    .pause(1, 2)
    .exec(
      http("Get Products with Category Filter")
        .get("/api/products")
        .queryParam("categoryId", "${categoryId}")
        .queryParam("page", "0")
        .queryParam("size", "20")
        .check(status.is(200))
    )
    .pause(1, 2)

  // Setup simulation with different load patterns
  setUp(
    // Ramp-up users over 30 seconds, maintain for 2 minutes
    browseProducts.inject(
      rampUsersPerSec(1).to(10).during(30.seconds),
      constantUsersPerSec(10).during(2.minutes)
    ),
    
    // Constant load for search
    searchProducts.inject(
      constantUsersPerSec(5).during(3.minutes)
    ),
    
    // Spike test for CRUD operations
    productCRUD.inject(
      rampUsersPerSec(0.5).to(2).during(30.seconds),
      constantUsersPerSec(2).during(2.minutes),
      rampUsersPerSec(2).to(10).during(30.seconds),
      constantUsersPerSec(10).during(1.minute)
    ),
    
    // Steady load for category browsing
    productsByCategory.inject(
      constantUsersPerSec(3).during(3.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(2000), // Max response time < 2s
      global.responseTime.mean.lt(500), // Mean response time < 500ms
      global.successfulRequests.percent.gt(95) // Success rate > 95%
    )
}
