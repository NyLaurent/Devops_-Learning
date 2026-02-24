# Complete Guide to Load Testing: Gatling & JMeter
## From Beginner to Professional

---

## Table of Contents

1. [Introduction to Load Testing](#introduction-to-load-testing)
2. [Gatling Tutorial](#gatling-tutorial)
   - [Beginner Level](#gatling-beginner)
   - [Intermediate Level](#gatling-intermediate)
   - [Advanced/Professional Level](#gatling-advanced)
3. [JMeter Tutorial](#jmeter-tutorial)
4. [Gatling vs JMeter Comparison](#gatling-vs-jmeter)
5. [Best Practices](#best-practices)
6. [Real-World Examples](#real-world-examples)

---

## Introduction to Load Testing

### What is Load Testing?

Load testing is a type of performance testing that simulates real-world load on a software application to determine how it behaves under normal and peak conditions.

### Why Load Testing Matters

- **Performance Validation**: Ensure your application can handle expected traffic
- **Capacity Planning**: Understand system limits and plan for scaling
- **Bottleneck Identification**: Find performance issues before production
- **User Experience**: Ensure fast response times under load
- **Cost Optimization**: Right-size infrastructure based on actual needs

### Key Metrics

- **Response Time**: Time taken to process a request
- **Throughput**: Requests per second (RPS)
- **Error Rate**: Percentage of failed requests
- **Concurrent Users**: Number of simultaneous users
- **Resource Utilization**: CPU, memory, network usage

---

## Gatling Tutorial

### Gatling Beginner

#### What is Gatling?

Gatling is a modern, high-performance load testing tool written in Scala. It uses an asynchronous, non-blocking architecture that makes it very efficient.

**Key Features:**
- Code-based (DSL) test definitions
- High performance (can simulate thousands of users)
- Detailed HTML reports
- Real-time metrics
- CI/CD integration

#### Installation

**Prerequisites:**
- Java 8+ (Java 11+ recommended)
- Maven or Gradle (for project setup)

**Option 1: Maven Project**

```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.10.3</version>
</dependency>
```

**Option 2: Standalone Bundle**

Download from: https://gatling.io/open-source/

Extract and run:
```bash
./bin/gatling.sh
```

#### Your First Gatling Test

Create a file: `src/test/scala/BasicSimulation.scala`

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("Basic Test")
    .exec(
      http("Get Homepage")
        .get("/")
        .check(status.is(200))
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
```

**Running the Test:**

```bash
mvn gatling:test
```

#### Understanding the Code

1. **Simulation**: Extends `Simulation` - the base class for all Gatling tests
2. **HTTP Protocol**: Defines base URL and common headers
3. **Scenario**: Represents a user journey
4. **Request**: HTTP request with assertions (checks)
5. **setUp**: Configures how many users and load pattern

#### Basic Concepts

**Scenarios**
```scala
val scn = scenario("My Scenario")
  .exec(http("Request 1").get("/api/users"))
  .pause(2) // Wait 2 seconds
  .exec(http("Request 2").get("/api/products"))
```

**Checks (Assertions)**
```scala
.exec(
  http("Get User")
    .get("/api/users/1")
    .check(
      status.is(200),
      jsonPath("$.name").is("John"),
      responseTimeInMillis.lt(500)
    )
)
```

**User Injection**
```scala
// All at once
atOnceUsers(10)

// Ramp up over time
rampUsers(10).during(30.seconds)

// Constant rate
constantUsersPerSec(5).during(2.minutes)
```

#### Exercise: Create a Simple API Test

Test a REST API with these endpoints:
- GET `/api/users` - List users
- GET `/api/users/{id}` - Get user by ID
- POST `/api/users` - Create user

**Solution:**

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UserAPISimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val scn = scenario("User API Test")
    .exec(
      http("Get All Users")
        .get("/api/users")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Get User by ID")
        .get("/api/users/1")
        .check(
          status.is(200),
          jsonPath("$.id").is("1")
        )
    )
    .pause(1)
    .exec(
      http("Create User")
        .post("/api/users")
        .body(StringBody("""{"name":"Test User","email":"test@example.com"}"""))
        .check(
          status.is(201),
          jsonPath("$.id").saveAs("userId")
        )
    )
    .exec(
      http("Get Created User")
        .get("/api/users/${userId}")
        .check(status.is(200))
    )

  setUp(
    scn.inject(
      rampUsers(10).during(30.seconds),
      constantUsersPerSec(5).during(2.minutes)
    )
  ).protocols(httpProtocol)
}
```

---

### Gatling Intermediate

#### Feeder Files (Test Data)

Use CSV files for dynamic test data:

**data/users.csv:**
```csv
userId,userName,email
1,John,john@example.com
2,Jane,jane@example.com
3,Bob,bob@example.com
```

**Using Feeders:**
```scala
val userFeeder = csv("data/users.csv").circular

val scn = scenario("User Test")
  .feed(userFeeder)
  .exec(
    http("Get User")
      .get("/api/users/${userId}")
      .check(status.is(200))
  )
```

**Feeder Types:**
- `circular`: Loops through data
- `random`: Random selection
- `queue`: Sequential, fails when empty
- `shuffle`: Random order, then loops

#### Session Variables

Store and reuse values:

```scala
.exec(
  http("Create User")
    .post("/api/users")
    .body(StringBody("""{"name":"${userName}"}"""))
    .check(
      jsonPath("$.id").saveAs("newUserId")
    )
)
.exec(
  http("Get Created User")
    .get("/api/users/${newUserId}")
)
```

#### Conditional Logic

```scala
.doIf("${userId.exists()}") {
  exec(http("Get User").get("/api/users/${userId}"))
}
.doIfEquals("${status}", "active") {
  exec(http("Activate").post("/api/activate"))
}
```

#### Loops and Repeats

```scala
.repeat(10) {
  exec(http("Get Products").get("/api/products"))
    .pause(1)
}

.repeat(10, "counter") {
  exec(
    http("Get Product ${counter}")
      .get("/api/products/${counter}")
  )
}
```

#### Groups and Checks

```scala
.exec(
  http("Complex Request")
    .get("/api/data")
    .check(
      status.in(200, 201, 204),
      jsonPath("$[*].id").findAll.saveAs("ids"),
      bodyString.saveAs("responseBody")
    )
)
.exec { session =>
  val ids = session("ids").as[Seq[String]]
  println(s"Found ${ids.size} items")
  session
}
```

#### Advanced User Injection

**Open Model (Rate-based):**
```scala
// Ramp rate
rampUsersPerSec(1).to(10).during(1.minute)

// Constant rate
constantUsersPerSec(5).during(5.minutes)

// Spike
rampUsersPerSec(1).to(50).during(10.seconds),
constantUsersPerSec(50).during(1.minute),
rampUsersPerSec(50).to(1).during(10.seconds)
```

**Closed Model (Concurrent users):**
```scala
// Ramp concurrent users
rampConcurrentUsers(10).during(30.seconds)

// Constant concurrent users
constantConcurrentUsers(20).during(2.minutes)

// Increment pattern
incrementConcurrentUsers(10)
  .times(5)
  .eachLevelLasting(30.seconds)
  .separatedByRampsLasting(10.seconds)
  .startingFrom(10)
```

#### Assertions

```scala
setUp(
  scn.inject(rampUsers(100).during(1.minute))
).protocols(httpProtocol)
  .assertions(
    global.responseTime.max.lt(2000),        // Max < 2s
    global.responseTime.mean.lt(500),         // Mean < 500ms
    global.responseTime.percentile3.lt(1000), // 75th < 1s
    global.successfulRequests.percent.gt(95), // Success > 95%
    forAll.failedRequests.percent.lt(5)       // Failures < 5%
  )
```

#### Exercise: E-Commerce Flow

Create a test that:
1. Browses products
2. Adds to cart
3. Checks out
4. Uses feeders for product data

**Solution:**

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class EcommerceSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val productFeeder = csv("data/products.csv").random

  val scn = scenario("E-Commerce Flow")
    .exec(
      http("Browse Products")
        .get("/api/products")
        .check(
          status.is(200),
          jsonPath("$[0].id").saveAs("productId")
        )
    )
    .pause(2)
    .feed(productFeeder)
    .exec(
      http("View Product")
        .get("/api/products/${productId}")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Add to Cart")
        .post("/api/cart")
        .body(StringBody("""{"productId":"${productId}","quantity":1}"""))
        .check(
          status.is(201),
          jsonPath("$.cartId").saveAs("cartId")
        )
    )
    .pause(2)
    .exec(
      http("Checkout")
        .post("/api/checkout/${cartId}")
        .body(StringBody("""{"paymentMethod":"credit_card"}"""))
        .check(status.is(200))
    )

  setUp(
    scn.inject(
      rampUsersPerSec(1).to(10).during(1.minute),
      constantUsersPerSec(10).during(5.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(500),
      global.successfulRequests.percent.gt(95)
    )
}
```

---

### Gatling Advanced

#### Custom Request Bodies

**JSON Templates:**
```scala
val createUserBody = StringBody(
  """{
    "name": "${userName}",
    "email": "${email}",
    "age": ${age}
  }"""
)
```

**ElFileBody (External Files):**
```scala
// templates/user.json
{
  "name": "${userName}",
  "email": "${email}"
}

// In simulation
.body(ElFileBody("templates/user.json"))
```

**RawFileBody:**
```scala
.body(RawFileBody("payloads/large-request.json"))
```

#### Authentication

**Basic Auth:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .basicAuth("username", "password")
```

**Token-based:**
```scala
val login = exec(
  http("Login")
    .post("/api/login")
    .body(StringBody("""{"username":"user","password":"pass"}"""))
    .check(
      jsonPath("$.token").saveAs("authToken")
    )
)

val authenticatedRequest = exec(
  http("Get Profile")
    .get("/api/profile")
    .header("Authorization", "Bearer ${authToken}")
)
```

**OAuth2:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .oauth2("your-access-token")
```

#### WebSocket Support

```scala
val wsProtocol = ws
  .baseUrl("ws://localhost:8080")
  .subprotocol("chat")

val scn = scenario("WebSocket Test")
  .exec(ws("Connect").connect("/chat"))
  .pause(1)
  .exec(ws("Send Message")
    .sendText("Hello Server"))
  .exec(ws("Wait for Response")
    .check(wsAwait.within(10).until(1).regex(".*response.*")))
  .exec(ws("Close").close)
```

#### Response Processing

**Extract and Transform:**
```scala
.exec(
  http("Get Data")
    .get("/api/data")
    .check(
      jsonPath("$.items[*].id")
        .findAll
        .transform(_.map(_.toInt))
        .saveAs("itemIds")
    )
)
```

**Regex Extraction:**
```scala
.check(
  regex("""id="(\d+)"""").saveAs("extractedId")
)
```

**XPath (for XML):**
```scala
.check(
  xpath("//user[@id='1']/name").saveAs("userName")
)
```

#### Custom Functions

```scala
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder

def randomString(length: Int): String = {
  scala.util.Random.alphanumeric.take(length).mkString
}

val createUser = exec { session =>
  val email = s"user${randomString(8)}@example.com"
  session.set("email", email)
}
.exec(
  http("Create User")
    .post("/api/users")
    .body(StringBody("""{"email":"${email}"}"""))
)
```

#### Resource Management

**Connection Pooling:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .shareConnections
  .maxConnectionsPerHost(10)
```

**Request Timeouts:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .requestTimeout(5000) // 5 seconds
  .connectionTimeout(3000) // 3 seconds
```

#### Distributed Testing

**Gatling Enterprise (Commercial):**
- Distributed execution across multiple nodes
- Centralized reporting
- Real-time monitoring

**Manual Distribution:**
```bash
# On multiple machines
./gatling.sh -s MySimulation -rf /shared/results
# Aggregate results manually
```

#### Performance Optimization

**Reduce Logging:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .silentUri(".*\\.(css|js|png|jpg|gif|ico).*")
```

**Disable Warm-up:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .disableWarmUp
```

**Optimize Checks:**
```scala
// Only check what you need
.check(status.is(200)) // Lightweight
// vs
.check(bodyString.saveAs("response")) // Heavy
```

#### Advanced Assertions

```scala
setUp(
  scn.inject(rampUsers(100).during(1.minute))
).protocols(httpProtocol)
  .assertions(
    // Global assertions
    global.responseTime.max.lt(2000),
    global.responseTime.mean.lt(500),
    
    // Per request
    details("Get Users").responseTime.mean.lt(300),
    details("Create User").successfulRequests.percent.gt(99),
    
    // Response time percentiles
    global.responseTime.percentile1.lt(200),  // 50th percentile
    global.responseTime.percentile2.lt(500), // 75th percentile
    global.responseTime.percentile3.lt(1000), // 95th percentile
    global.responseTime.percentile4.lt(2000), // 99th percentile
    
    // Response time distribution
    global.responseTime.min.gt(0),
    global.responseTime.max.lt(5000),
    
    // Throughput
    global.requestsPerSec.gt(100),
    
    // Error rates
    forAll.failedRequests.percent.lt(1),
    details("Get Users").failedRequests.count.lt(5)
  )
```

#### CI/CD Integration

**Maven:**
```xml
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>4.4.0</version>
  <configuration>
    <simulationClass>com.example.MySimulation</simulationClass>
  </configuration>
</plugin>
```

**Gradle:**
```gradle
plugins {
    id 'io.gatling.gradle' version '3.10.3'
}
```

**GitLab CI:**
```yaml
load-test:
  stage: test
  script:
    - mvn gatling:test
  artifacts:
    paths:
      - target/gatling/**/*
```

#### Monitoring and Debugging

**Enable Debug Logging:**
```scala
// In logback.xml
<logger name="io.gatling" level="DEBUG"/>
```

**Session Debugging:**
```scala
.exec { session =>
  println(s"Session: ${session}")
  session
}
```

**Request/Response Logging:**
```scala
val httpProtocol = http
  .baseUrl("http://localhost:8080")
  .enableHttp2
  .logRequest
  .logResponse
```

---

## JMeter Tutorial

### What is JMeter?

Apache JMeter is an open-source Java application designed to load test functional behavior and measure performance. It's GUI-based and widely used in the industry.

### Installation

**Download:**
- Visit: https://jmeter.apache.org/download_jmeter.cgi
- Download the binary zip/tar.gz
- Extract and run: `./bin/jmeter.sh` (Linux/Mac) or `bin\jmeter.bat` (Windows)

**Requirements:**
- Java 8+ (Java 11+ recommended)

### JMeter GUI Basics

#### Main Components

1. **Test Plan**: Root element containing all test elements
2. **Thread Group**: Defines users and load pattern
3. **Samplers**: Send requests (HTTP, FTP, JDBC, etc.)
4. **Listeners**: Display results (graphs, tables, trees)
5. **Config Elements**: Set defaults (HTTP Request Defaults, CSV Data Set Config)
6. **Pre/Post Processors**: Modify requests/responses
7. **Assertions**: Validate responses
8. **Timers**: Add delays

#### Your First JMeter Test

**Step 1: Create Test Plan**
- Right-click Test Plan → Add → Threads (Users) → Thread Group

**Step 2: Configure Thread Group**
- Number of Threads: 10
- Ramp-up Period: 30 (seconds)
- Loop Count: 5

**Step 3: Add HTTP Request**
- Right-click Thread Group → Add → Sampler → HTTP Request
- Server Name: `localhost`
- Port: `8080`
- Path: `/api/users`
- Method: `GET`

**Step 4: Add Listener**
- Right-click Thread Group → Add → Listener → View Results Tree
- Right-click Thread Group → Add → Listener → Summary Report

**Step 5: Run Test**
- Click green play button (Ctrl+R)
- View results in listeners

### JMeter Concepts

#### Thread Groups

**Setup Thread Group:**
- Runs before main test (preparation)

**Thread Group:**
- Main test execution
- Configurable: users, ramp-up, loops

**Teardown Thread Group:**
- Runs after main test (cleanup)

**Configuration:**
- Number of Threads (users)
- Ramp-up Period (seconds to start all users)
- Loop Count (iterations per user)
- Same user on each iteration (maintain session)

#### HTTP Request Sampler

**Basic Configuration:**
- Protocol: `http` or `https`
- Server Name: `api.example.com`
- Port: `8080`
- Method: `GET`, `POST`, `PUT`, `DELETE`
- Path: `/api/users`

**Advanced:**
- Parameters (query string or form data)
- Body Data (for POST/PUT)
- Files Upload
- Follow Redirects
- Use KeepAlive

#### Controllers

**Simple Controller:**
- Groups elements (organizational)

**Loop Controller:**
- Repeats child elements N times

**If Controller:**
- Conditional execution based on condition

**While Controller:**
- Loops while condition is true

**Transaction Controller:**
- Groups multiple samplers as one transaction
- Measures total time

**Random Controller:**
- Executes one random child element

#### Config Elements

**HTTP Request Defaults:**
- Sets default values for all HTTP requests
- Server, port, protocol

**HTTP Header Manager:**
- Adds headers to requests
- Content-Type, Authorization, etc.

**CSV Data Set Config:**
- Reads data from CSV file
- Variables: `${username}`, `${password}`

**User Defined Variables:**
- Define variables: `${baseUrl}`, `${apiKey}`

**HTTP Cookie Manager:**
- Handles cookies automatically
- Maintains session

#### Assertions

**Response Assertion:**
- Check status code, response body, headers
- Contains, Equals, Matches (regex)

**JSON Assertion:**
- Validate JSON response
- JSONPath expressions

**Duration Assertion:**
- Response time threshold

**Size Assertion:**
- Response size validation

#### Timers

**Constant Timer:**
- Fixed delay: 1000ms

**Uniform Random Timer:**
- Random delay: 1000-3000ms

**Gaussian Random Timer:**
- Normal distribution delay

**Synchronizing Timer:**
- Waits until N threads ready (barrier)

#### Pre/Post Processors

**Pre-Processors (Before Request):**
- **User Parameters**: Set variables
- **BeanShell PreProcessor**: Custom script
- **Regular Expression Extractor**: Extract from previous response

**Post-Processors (After Request):**
- **Regular Expression Extractor**: Extract values
- **JSON Extractor**: Extract from JSON
- **XPath Extractor**: Extract from XML
- **BeanShell PostProcessor**: Custom script

#### Listeners

**View Results Tree:**
- Detailed request/response
- Good for debugging
- **Disable in load tests** (high overhead)

**Summary Report:**
- Aggregated statistics
- Min, Max, Avg, Error %

**Aggregate Report:**
- Similar to Summary Report
- Additional percentiles

**Graph Results:**
- Visual graph of response times

**Response Times Over Time:**
- Response time vs time graph

**Active Threads Over Time:**
- Concurrent users graph

### Advanced JMeter

#### Parameterization

**CSV Data Set Config:**
```
# users.csv
username,password,email
user1,pass1,user1@example.com
user2,pass2,user2@example.com
```

**Configuration:**
- Filename: `users.csv`
- Variable Names: `username,password,email`
- Delimiter: `,`
- Recycle on EOF: `True`
- Stop thread on EOF: `False`
- Sharing mode: `All threads`

**Usage:**
```
POST /api/login
Body: {"username":"${username}","password":"${password}"}
```

#### Correlation (Extract and Reuse)

**Extract Token:**
1. Add Regular Expression Extractor
2. Apply to: `HTTP Request`
3. Reference Name: `authToken`
4. Regular Expression: `"token":"(.+?)"`
5. Template: `$1$`
6. Match No.: `1`

**Use Token:**
```
GET /api/profile
Header: Authorization: Bearer ${authToken}
```

#### Functions and Variables

**Built-in Functions:**
- `${__time()}` - Current timestamp
- `${__Random(1,100)}` - Random number
- `${__UUID()}` - Generate UUID
- `${__threadNum}` - Thread number
- `${__counter()}` - Counter
- `${__V(varname)}` - Variable reference

**User-Defined Variables:**
- Test Plan → User Defined Variables
- Name: `baseUrl`, Value: `http://localhost:8080`
- Use: `${baseUrl}/api/users`

#### Distributed Testing

**Master-Slave Setup:**

**Master (Controller):**
1. Edit `bin/jmeter.properties`
2. Set: `remote_hosts=slave1:1099,slave2:1099`
3. Run: `./jmeter-server.sh` on slaves
4. Run test from master GUI: Run → Remote Start

**Command Line:**
```bash
jmeter -n -t test.jmx -R slave1,slave2 -l results.jtl
```

#### Non-GUI Mode (CLI)

**Run Test:**
```bash
jmeter -n -t test.jmx -l results.jtl -e -o report/
```

**Options:**
- `-n`: Non-GUI mode
- `-t`: Test plan file
- `-l`: Results file
- `-e`: Generate HTML report
- `-o`: Output directory

**Generate Report:**
```bash
jmeter -g results.jtl -o report/
```

#### Best Practices

1. **Disable View Results Tree in Load Tests**
   - High memory overhead
   - Use Summary Report instead

2. **Use CSV Data Set Config for Test Data**
   - Avoid hardcoding values
   - Easy to maintain

3. **Use HTTP Request Defaults**
   - Avoid repetition
   - Centralized configuration

4. **Add Timers Realistically**
   - Simulate user think time
   - Use random timers

5. **Use Transaction Controller**
   - Group related requests
   - Measure business transactions

6. **Run in Non-GUI Mode**
   - Lower resource usage
   - Better for CI/CD

7. **Monitor System Resources**
   - CPU, Memory, Network
   - Don't overload JMeter itself

#### JMeter Plugins

**Install Plugin Manager:**
1. Download `jmeter-plugins-manager.jar`
2. Place in `lib/ext/`
3. Restart JMeter

**Popular Plugins:**
- **Custom Thread Groups**: Ultimate Thread Group, Stepping Thread Group
- **Listeners**: Response Times Over Time, Active Threads Over Time
- **Samplers**: HTTP/2, WebSocket
- **Functions**: Additional functions

---

## Gatling vs JMeter Comparison

### Feature Comparison

| Feature | Gatling | JMeter |
|--------|---------|--------|
| **Language** | Scala (DSL) | Java (GUI/XML) |
| **Learning Curve** | Steeper (requires coding) | Easier (GUI-based) |
| **Performance** | Very High (async, non-blocking) | Good (thread-based) |
| **Resource Usage** | Lower | Higher |
| **Code Versioning** | Excellent (code in Git) | Good (XML files) |
| **CI/CD Integration** | Excellent | Good |
| **Reporting** | Excellent HTML reports | Good (requires plugins) |
| **Distributed Testing** | Enterprise (paid) | Free (built-in) |
| **Community** | Growing | Large, mature |
| **Documentation** | Good | Excellent |
| **WebSocket Support** | Built-in | Plugin required |
| **HTTP/2 Support** | Built-in | Plugin required |
| **Real-time Monitoring** | Limited | Good (plugins) |
| **Test Data Management** | Code-based | CSV, Database |
| **Debugging** | Code debugging | GUI inspection |

### Performance Comparison

**Resource Efficiency:**
- **Gatling**: Can simulate 10,000+ users on a single machine
- **JMeter**: Typically 500-1000 users per machine (depends on hardware)

**Why Gatling is More Efficient:**
- Asynchronous, non-blocking I/O
- Event-driven architecture
- Lower memory footprint
- Better CPU utilization

**Example:**
```
Test: 1000 concurrent users, 5-minute test

Gatling:
- CPU: 30-40%
- Memory: 2-3 GB
- Throughput: 5000 req/s

JMeter:
- CPU: 60-80%
- Memory: 4-6 GB
- Throughput: 2000 req/s
```

### Use Case Recommendations

#### Choose Gatling When:

✅ **High Performance Requirements**
- Need to simulate thousands of users
- Limited hardware resources
- High throughput requirements

✅ **Code-First Approach**
- Team comfortable with coding
- Want version control for tests
- Prefer code reviews

✅ **CI/CD Integration**
- Automated testing in pipelines
- Need programmatic test execution
- Want test results as artifacts

✅ **Modern Protocols**
- HTTP/2, WebSocket support needed
- Async protocols
- Real-time applications

✅ **Scalable Testing**
- Growing test suite
- Multiple environments
- Need maintainable tests

#### Choose JMeter When:

✅ **Quick Start**
- Need to create tests quickly
- Non-technical team members
- Rapid prototyping

✅ **GUI Preference**
- Prefer visual test creation
- Want to see requests/responses
- Interactive debugging

✅ **Mature Ecosystem**
- Large plugin library
- Extensive documentation
- Large community support

✅ **Distributed Testing**
- Need free distributed testing
- Multiple test machines
- Centralized results

✅ **Protocol Variety**
- Need many protocol samplers
- Database, FTP, LDAP testing
- Legacy system testing

### Migration Guide

#### From JMeter to Gatling

**Step 1: Identify Test Scenarios**
- List all Thread Groups
- Document user journeys
- Note test data sources

**Step 2: Map Elements**
```
JMeter                    →  Gatling
Thread Group              →  Scenario
HTTP Request              →  http(...).get/post
CSV Data Set Config       →  csv("file.csv")
Regular Expression Extractor → .check(jsonPath(...))
Timer                     →  .pause()
Assertion                 →  .check(...)
```

**Step 3: Convert Test Plan**
```scala
// JMeter: Thread Group (10 users, 30s ramp, 5 loops)
// Gatling:
setUp(
  scn.inject(
    rampUsers(10).during(30.seconds),
    constantUsersPerSec(10).during(5.minutes)
  )
)
```

**Step 4: Migrate Test Data**
```scala
// JMeter: CSV Data Set Config
// Gatling:
val userFeeder = csv("users.csv").circular
```

**Step 5: Convert Assertions**
```scala
// JMeter: Response Assertion (status = 200)
// Gatling:
.check(status.is(200))
```

### Side-by-Side Example

**Test: Login and Get Profile**

**JMeter:**
```
Test Plan
├── Thread Group (10 users, 30s ramp)
│   ├── HTTP Request Defaults (server: localhost:8080)
│   ├── CSV Data Set Config (users.csv)
│   ├── HTTP Request (POST /api/login)
│   │   ├── Body Data: {"username":"${username}","password":"${password}"}
│   │   └── Regular Expression Extractor (token)
│   ├── HTTP Request (GET /api/profile)
│   │   └── Header Manager (Authorization: Bearer ${token})
│   └── Summary Report
```

**Gatling:**
```scala
class LoginSimulation extends Simulation {
  val httpProtocol = http.baseUrl("http://localhost:8080")
  val userFeeder = csv("users.csv").circular
  
  val scn = scenario("Login")
    .feed(userFeeder)
    .exec(
      http("Login")
        .post("/api/login")
        .body(StringBody("""{"username":"${username}","password":"${password}"}"""))
        .check(jsonPath("$.token").saveAs("token"))
    )
    .exec(
      http("Get Profile")
        .get("/api/profile")
        .header("Authorization", "Bearer ${token}")
    )
  
  setUp(
    scn.inject(rampUsers(10).during(30.seconds))
  ).protocols(httpProtocol)
}
```

---

## Best Practices

### General Load Testing

1. **Start Small**
   - Begin with low load
   - Gradually increase
   - Monitor system behavior

2. **Test Realistic Scenarios**
   - Model actual user behavior
   - Include think times
   - Use realistic test data

3. **Monitor System Resources**
   - CPU, Memory, Disk I/O
   - Network bandwidth
   - Database performance

4. **Baseline First**
   - Establish performance baseline
   - Document expected metrics
   - Set realistic targets

5. **Test Incrementally**
   - Single endpoint → Full flow
   - Low load → High load
   - Simple → Complex scenarios

### Gatling Best Practices

1. **Use Feeders for Test Data**
   ```scala
   val userFeeder = csv("users.csv").circular
   ```

2. **Organize with Scenarios**
   ```scala
   val browseScenario = scenario("Browse")
   val purchaseScenario = scenario("Purchase")
   ```

3. **Reuse HTTP Protocol**
   ```scala
   val httpProtocol = http.baseUrl("http://api.example.com")
   ```

4. **Add Realistic Pauses**
   ```scala
   .pause(2, 5) // Random 2-5 seconds
   ```

5. **Use Assertions**
   ```scala
   .assertions(
     global.responseTime.mean.lt(500)
   )
   ```

6. **Disable View Results Tree in Production**
   - High overhead
   - Use Summary Report

### JMeter Best Practices

1. **Use Non-GUI Mode for Load Tests**
   ```bash
   jmeter -n -t test.jmx -l results.jtl
   ```

2. **Disable Unnecessary Listeners**
   - View Results Tree (debugging only)
   - Use Summary Report

3. **Use CSV Data Set Config**
   - Avoid hardcoded values
   - Easy to maintain

4. **Add Timers**
   - Simulate user think time
   - Use random timers

5. **Use Transaction Controllers**
   - Group related requests
   - Measure business transactions

6. **Monitor JMeter Resources**
   - Don't overload JMeter
   - Use distributed testing if needed

### Common Pitfalls

1. **Testing from Wrong Location**
   - Test from same network as users
   - Consider geographic distribution

2. **Ignoring Think Time**
   - Users don't click instantly
   - Add realistic pauses

3. **Testing with Cached Data**
   - Clear caches between tests
   - Use fresh test data

4. **Not Monitoring System**
   - Monitor server resources
   - Check database performance
   - Watch network utilization

5. **Unrealistic Load Patterns**
   - Gradual ramp-up
   - Sustained load periods
   - Realistic spike patterns

---

## Real-World Examples

### Example 1: E-Commerce Load Test

**Scenario:** Test product browsing and purchase flow

**Gatling:**
```scala
class EcommerceLoadTest extends Simulation {
  val httpProtocol = http.baseUrl("https://api.store.com")
  val productFeeder = csv("products.csv").random
  
  val browseFlow = scenario("Browse Products")
    .exec(http("Homepage").get("/"))
    .pause(2)
    .exec(http("Products").get("/api/products"))
    .pause(1, 3)
    .feed(productFeeder)
    .exec(http("Product Detail").get("/api/products/${productId}"))
  
  val purchaseFlow = scenario("Purchase")
    .feed(productFeeder)
    .exec(http("Add to Cart").post("/api/cart").body(StringBody("""{"productId":"${productId}"}""")))
    .pause(1)
    .exec(http("Checkout").post("/api/checkout"))
  
  setUp(
    browseFlow.inject(rampUsersPerSec(1).to(50).during(5.minutes)),
    purchaseFlow.inject(rampUsersPerSec(0.1).to(5).during(5.minutes))
  ).protocols(httpProtocol)
}
```

### Example 2: API Stress Test

**Scenario:** Find maximum throughput

**Gatling:**
```scala
class APIStressTest extends Simulation {
  val httpProtocol = http.baseUrl("http://api.example.com")
  
  val scn = scenario("API Stress")
    .exec(http("Get Data").get("/api/data"))
  
  setUp(
    scn.inject(
      rampUsersPerSec(1).to(100).during(2.minutes),
      constantUsersPerSec(100).during(10.minutes),
      rampUsersPerSec(100).to(200).during(2.minutes),
      constantUsersPerSec(200).during(10.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(1000),
      global.successfulRequests.percent.gt(99)
    )
}
```

### Example 3: Spike Test

**Scenario:** Test system behavior under sudden load spike

**Gatling:**
```scala
class SpikeTest extends Simulation {
  val httpProtocol = http.baseUrl("http://api.example.com")
  
  val scn = scenario("Spike")
    .exec(http("Request").get("/api/endpoint"))
  
  setUp(
    scn.inject(
      nothingFor(30.seconds),
      rampUsersPerSec(1).to(10).during(1.minute),
      constantUsersPerSec(10).during(5.minutes),
      rampUsersPerSec(10).to(500).during(30.seconds), // Spike!
      constantUsersPerSec(500).during(2.minutes),
      rampUsersPerSec(500).to(10).during(30.seconds),
      constantUsersPerSec(10).during(5.minutes)
    )
  ).protocols(httpProtocol)
}
```

---

## Conclusion

Both Gatling and JMeter are powerful load testing tools with their own strengths:

- **Gatling** excels in performance, code maintainability, and CI/CD integration
- **JMeter** shines in ease of use, GUI-based testing, and distributed testing

Choose based on your team's skills, requirements, and constraints. Many organizations use both tools for different purposes.

### Next Steps

1. **Practice**: Create tests for your own APIs
2. **Experiment**: Try different load patterns
3. **Monitor**: Learn to interpret results
4. **Optimize**: Use results to improve application performance
5. **Automate**: Integrate into CI/CD pipelines

### Resources

**Gatling:**
- Official Docs: https://gatling.io/docs/
- Examples: https://github.com/gatling/gatling/tree/main/gatling-bundle/src/main/scala/io/gatling/examples

**JMeter:**
- Official Docs: https://jmeter.apache.org/usermanual/
- Best Practices: https://jmeter.apache.org/usermanual/best-practices.html

**Both:**
- Load Testing Best Practices: https://www.blazemeter.com/blog/load-testing-best-practices

---

*Happy Load Testing! 🚀*
