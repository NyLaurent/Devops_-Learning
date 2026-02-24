package com.ecommerce.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Comprehensive E-Commerce simulation combining all scenarios
 * This is the main simulation that runs all test scenarios together
 */
class EcommerceSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")
    .shareConnections
    .disableWarmUp

  // Feeder files
  val productFeeder = csv("data/products.csv").circular
  val categoryFeeder = csv("data/categories.csv").circular
  val searchFeeder = csv("data/search-terms.csv").circular

  // Scenario: Complete User Journey
  val userJourney = scenario("Complete User Journey")
    .exec(
      http("Health Check")
        .get("/actuator/health")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Get All Categories")
        .get("/api/categories")
        .check(status.is(200))
        .check(jsonPath("$[0].id").saveAs("firstCategoryId"))
    )
    .pause(1, 2)
    .exec(
      http("Get Products by Category")
        .get("/api/products/category/${firstCategoryId}")
        .check(status.in(200, 404))
        .check(jsonPath("$[0].id").optional.saveAs("firstProductId"))
    )
    .pause(1, 2)
    .doIf("${firstProductId.exists()}") {
      exec(
        http("Get Product Details")
          .get("/api/products/${firstProductId}")
          .check(status.is(200))
      )
    }
    .pause(1, 2)
    .exec(
      http("Search Products")
        .get("/api/products")
        .queryParam("search", "laptop")
        .queryParam("page", "0")
        .queryParam("size", "10")
        .check(status.is(200))
    )
    .pause(2, 4)

  // Scenario: Stress Test - High Load
  val stressTest = scenario("Stress Test")
    .feed(productFeeder)
    .exec(
      http("Get All Products")
        .get("/api/products")
        .queryParam("page", "0")
        .queryParam("size", "50")
        .check(status.is(200))
    )
    .pause(500.milliseconds, 1.second)
    .exec(
      http("Get Product by ID")
        .get("/api/products/${productId}")
        .check(status.in(200, 404))
    )
    .pause(500.milliseconds, 1.second)
    .exec(
      http("Get All Categories")
        .get("/api/categories")
        .check(status.is(200))
    )

  // Scenario: Endurance Test - Long Running
  val enduranceTest = scenario("Endurance Test")
    .repeat(100) {
      exec(
        http("Get Products")
          .get("/api/products")
          .queryParam("page", "0")
          .queryParam("size", "20")
          .check(status.is(200))
      )
      .pause(2, 5)
      .exec(
        http("Get Categories")
          .get("/api/categories")
          .check(status.is(200))
      )
      .pause(2, 5)
    }

  // Setup simulation based on system property
  val simulationType = System.getProperty("simulationType", "normal")

  val setup = simulationType match {
    case "stress" =>
      setUp(
        stressTest.inject(
          rampUsersPerSec(10).to(50).during(1.minute),
          constantUsersPerSec(50).during(5.minutes),
          rampUsersPerSec(50).to(100).during(1.minute),
          constantUsersPerSec(100).during(5.minutes)
        )
      )
    
    case "endurance" =>
      setUp(
        enduranceTest.inject(
          constantUsersPerSec(5).during(30.minutes)
        )
      )
    
    case "spike" =>
      setUp(
        userJourney.inject(
          nothingFor(10.seconds),
          rampUsersPerSec(1).to(5).during(30.seconds),
          constantUsersPerSec(5).during(2.minutes),
          rampUsersPerSec(5).to(50).during(30.seconds), // Spike
          constantUsersPerSec(50).during(1.minute),
          rampUsersPerSec(50).to(5).during(30.seconds),
          constantUsersPerSec(5).during(2.minutes)
        )
      )
    
    case _ => // normal
      setUp(
        userJourney.inject(
          rampUsersPerSec(1).to(10).during(30.seconds),
          constantUsersPerSec(10).during(3.minutes)
        ),
        stressTest.inject(
          rampUsersPerSec(5).to(10).during(1.minute),
          constantUsersPerSec(10).during(2.minutes)
        )
      )
  }

  setup.protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(3000), // Max response time < 3s
      global.responseTime.mean.lt(800), // Mean response time < 800ms
      global.responseTime.percentile3.lt(1500), // 75th percentile < 1.5s
      global.successfulRequests.percent.gt(95), // Success rate > 95%
      forAll.failedRequests.percent.lt(5) // Failed requests < 5%
    )
}
