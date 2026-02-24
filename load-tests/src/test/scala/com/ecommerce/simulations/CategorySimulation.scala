package com.ecommerce.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CategorySimulation extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  // Feeder for category data
  val categoryFeeder = csv("data/categories.csv").circular

  // Scenario: Get All Categories
  val getAllCategories = scenario("Get All Categories")
    .exec(
      http("Get All Categories")
        .get("/api/categories")
        .check(status.is(200))
        .check(jsonPath("$[*].id").findAll.saveAs("categoryIds"))
    )
    .pause(1, 2)

  // Scenario: Get Category by ID
  val getCategoryById = scenario("Get Category by ID")
    .feed(categoryFeeder)
    .exec(
      http("Get Category by ID")
        .get("/api/categories/${categoryId}")
        .check(status.in(200, 404))
    )
    .pause(1, 2)

  // Scenario: Category CRUD Operations
  val categoryCRUD = scenario("Category CRUD Operations")
    .feed(categoryFeeder)
    .exec(
      http("Create Category")
        .post("/api/categories")
        .body(StringBody("""{
          "name": "${categoryName}",
          "description": "${categoryDescription}"
        }""")).asJson
        .check(status.in(201, 400))
        .check(jsonPath("$.id").optional.saveAs("createdCategoryId"))
    )
    .pause(1, 2)
    .doIf("${createdCategoryId.exists()}") {
      exec(
        http("Get Created Category")
          .get("/api/categories/${createdCategoryId}")
          .check(status.is(200))
      )
      .pause(1, 2)
      .exec(
        http("Update Category")
          .put("/api/categories/${createdCategoryId}")
          .body(StringBody("""{
            "name": "${categoryName} - Updated",
            "description": "${categoryDescription}"
          }""")).asJson
          .check(status.in(200, 404))
      )
      .pause(1, 2)
      .exec(
        http("Delete Category")
          .delete("/api/categories/${createdCategoryId}")
          .check(status.in(204, 404))
      )
    }

  // Setup simulation
  setUp(
    // High load for reading categories
    getAllCategories.inject(
      rampUsersPerSec(1).to(20).during(30.seconds),
      constantUsersPerSec(20).during(2.minutes)
    ),
    
    // Moderate load for getting specific categories
    getCategoryById.inject(
      constantUsersPerSec(10).during(3.minutes)
    ),
    
    // Low load for CRUD operations
    categoryCRUD.inject(
      rampUsersPerSec(0.1).to(1).during(20.seconds),
      constantUsersPerSec(1).during(2.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(1000), // Max response time < 1s
      global.responseTime.mean.lt(200), // Mean response time < 200ms
      global.successfulRequests.percent.gt(98) // Success rate > 98%
    )
}
