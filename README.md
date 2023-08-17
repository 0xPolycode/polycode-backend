# Polycode Backend

## Development setup

Minimum required Java version for running Gradle: 11

### Build

Before building the application the database must be up and running in order to execute Flyway migrations and generate
jOOQ code. The database can be started via Docker by running `docker-compose up -d` inside the `docker` folder.

To build the application run `./gradlew build`

### Run

Before running the application, make sure that the database is up and running.

To run the application run `./gradlew bootRun`

## Tests

There are 3 tests sets:

- `test` for unit tests which do not start Spring Boot
- `integTest` for tests which run only partial Spring Boot
- `apiTest` for tests which run the entire Spring Boot application

To execute all tests run `./gradlew fullTest`
