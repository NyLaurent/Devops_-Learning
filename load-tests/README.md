# Load and Stress Testing with Gatling

This directory contains Gatling load and stress tests for the E-Commerce backend API.

## Overview

Gatling is a powerful load testing tool written in Scala. These tests simulate realistic user behavior and measure the performance of the backend API under various load conditions.

## Structure

```
load-tests/
├── src/
│   └── test/
│       ├── scala/
│       │   └── com/
│       │       └── ecommerce/
│       │           └── simulations/
│       │               ├── ProductSimulation.scala      # Product API tests
│       │               ├── CategorySimulation.scala     # Category API tests
│       │               └── EcommerceSimulation.scala    # Comprehensive tests
│       └── resources/
│           ├── gatling.conf                            # Gatling configuration
│           └── data/
│               ├── products.csv                        # Test data
│               ├── categories.csv                      # Test data
│               └── search-terms.csv                    # Test data
├── pom.xml                                             # Maven configuration
├── .gitlab-ci.yml                                      # CI/CD pipeline
└── README.md                                           # This file
```

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Scala 2.13 (managed by Maven)

## Running Tests Locally

### 1. Install Dependencies

```bash
mvn clean install
```

### 2. Run All Simulations

```bash
mvn gatling:test
```

### 3. Run Specific Simulation

```bash
mvn gatling:test -Dgatling.simulationClass=com.ecommerce.simulations.ProductSimulation
```

### 4. Run with Custom Base URL

```bash
mvn gatling:test -DbaseUrl=http://localhost:8080
```

### 5. Run Different Test Types

```bash
# Normal load test
mvn gatling:test -DsimulationType=normal

# Stress test
mvn gatling:test -DsimulationType=stress

# Spike test
mvn gatling:test -DsimulationType=spike

# Endurance test
mvn gatling:test -DsimulationType=endurance
```

## Test Scenarios

### ProductSimulation

Tests the Product API endpoints:
- **Browse Products**: GET all products with pagination
- **Search Products**: Search functionality
- **Product CRUD**: Create, Read, Update, Delete operations
- **Products by Category**: Filtering by category

**Load Pattern**: Mixed ramp-up and constant load

### CategorySimulation

Tests the Category API endpoints:
- **Get All Categories**: List all categories
- **Get Category by ID**: Retrieve specific category
- **Category CRUD**: Full CRUD operations

**Load Pattern**: High read load, low write load

### EcommerceSimulation

Comprehensive simulation combining all scenarios:
- **User Journey**: Complete user flow
- **Stress Test**: High concurrent load
- **Endurance Test**: Long-running test

**Test Types**:
- `normal`: Standard load test
- `stress`: High load stress test (100-200 users)
- `spike`: Sudden traffic spike simulation
- `endurance`: Long-running test (30 minutes)

## Configuration

### Base URL

Set the target server URL:

```bash
-DbaseUrl=http://staging.example.com:8080
```

### Simulation Type

Choose the test type:

```bash
-DsimulationType=normal|stress|spike|endurance
```

### Gatling Configuration

Edit `src/test/resources/gatling.conf` to customize:
- Response time thresholds
- Percentiles
- Logging levels
- HTTP settings

## Test Data

Test data is stored in CSV files under `src/test/resources/data/`:

- **products.csv**: Product test data
- **categories.csv**: Category test data
- **search-terms.csv**: Search query terms

You can modify these files to match your test data requirements.

## Results

After running tests, results are generated in:
```
target/gatling/[simulation-name]-[timestamp]/
```

### Viewing Results

1. **HTML Report**: Open `index.html` in the results directory
2. **Console Output**: Check Maven console for summary
3. **CI/CD Artifacts**: Results are stored as GitLab CI artifacts

### Key Metrics

- **Response Time**: Mean, max, percentiles (50th, 75th, 95th, 99th)
- **Throughput**: Requests per second
- **Success Rate**: Percentage of successful requests
- **Error Rate**: Failed requests percentage

## CI/CD Integration

The load tests are integrated into GitLab CI/CD:

### Manual Jobs

All load test jobs are **manual** (require manual trigger):

- `load-test:normal` - Normal load test
- `load-test:stress` - Stress test
- `load-test:spike` - Spike test
- `load-test:endurance` - Endurance test

### Running in CI/CD

1. Go to GitLab CI/CD > Pipelines
2. Click on a pipeline
3. Click the play button (▶) on the desired load test job
4. Set `BASE_URL` variable if needed (defaults to staging URL)

### Environment Variables

Configure in GitLab CI/CD variables:

- `BASE_URL`: Target server URL (default: `http://localhost:8080`)
- `CI_ENVIRONMENT_URL`: Auto-set by GitLab environments

## Best Practices

1. **Start Small**: Begin with normal load, then increase
2. **Monitor Resources**: Watch CPU, memory, and database during tests
3. **Test Realistic Scenarios**: Use realistic user behavior patterns
4. **Run Regularly**: Schedule load tests as part of your release process
5. **Compare Results**: Track performance over time to detect regressions
6. **Test in Staging**: Always test against staging before production

## Performance Targets

Default assertions (can be adjusted in simulations):

- **Max Response Time**: < 2-3 seconds
- **Mean Response Time**: < 500-800ms
- **Success Rate**: > 95%
- **Error Rate**: < 5%

## Troubleshooting

### Tests Fail to Connect

- Verify the backend is running
- Check the `baseUrl` is correct
- Ensure network connectivity

### Out of Memory Errors

- Increase Maven heap size: `export MAVEN_OPTS="-Xmx2g"`
- Reduce number of concurrent users
- Use distributed testing for very high load

### Slow Test Execution

- Check backend performance
- Reduce test duration
- Optimize simulation scenarios

## Advanced Usage

### Custom Simulations

Create new simulation files in `src/test/scala/com/ecommerce/simulations/`:

```scala
package com.ecommerce.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class MyCustomSimulation extends Simulation {
  // Your simulation code
}
```

### Distributed Testing

For very high load, consider:
- Running multiple Gatling instances
- Using Gatling Enterprise (commercial)
- Cloud-based load testing services

## Resources

- [Gatling Documentation](https://gatling.io/docs/)
- [Gatling Maven Plugin](https://gatling.io/docs/gatling/reference/current/extensions/maven_plugin/)
- [Gatling DSL Reference](https://gatling.io/docs/gatling/reference/current/core/dsl/)

## Support

For issues or questions:
1. Check the Gatling documentation
2. Review test logs in `target/gatling/`
3. Check backend logs for errors
4. Verify test data files are correct
