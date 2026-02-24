# Quick Start Guide - Gatling Load Tests

## Quick Commands

### Run Tests Locally

```bash
# Navigate to load-tests directory
cd load-tests

# Install dependencies
mvn clean install

# Run all simulations
mvn gatling:test

# Run specific simulation
mvn gatling:test -Dgatling.simulationClass=com.ecommerce.simulations.ProductSimulation

# Run with custom URL
mvn gatling:test -DbaseUrl=http://localhost:8080

# Run different test types
mvn gatling:test -DsimulationType=normal    # Normal load
mvn gatling:test -DsimulationType=stress    # Stress test
mvn gatling:test -DsimulationType=spike     # Spike test
mvn gatling:test -DsimulationType=endurance # Endurance test
```

### View Results

After running tests, open the HTML report:

```bash
# Find the latest results directory
ls -lt target/gatling/

# Open the HTML report (replace with actual directory name)
open target/gatling/ecommercesimulation-*/index.html
```

## Test Types Explained

- **normal**: Standard load test with gradual ramp-up
- **stress**: High concurrent load (100-200 users)
- **spike**: Sudden traffic spike simulation
- **endurance**: Long-running test (30 minutes)

## Prerequisites

- Java 21+
- Maven 3.6+
- Backend running on target URL

## Common Issues

### Backend Not Running
```bash
# Start backend first
cd ../backend
mvn spring-boot:run
```

### Out of Memory
```bash
export MAVEN_OPTS="-Xmx2g"
mvn gatling:test
```

### Wrong URL
```bash
# Use -DbaseUrl to specify correct URL
mvn gatling:test -DbaseUrl=http://staging.example.com:8080
```

## Next Steps

See [README.md](README.md) for detailed documentation.
