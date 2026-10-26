# Banking System

Spring Boot application for bank accounts, deposits, withdrawals, and transfers. It uses Java 17, MySQL, and JPA.

## Requirements

- JDK 17
- MySQL 8, listening on port 3306
- Maven is optional. This repository includes the Maven Wrapper (`mvnw.cmd`), which downloads the right Maven version for you.

Check Java:

```powershell
java -version
```

The reported version should be 17 or newer.

## Database

Start the MySQL service, then create the database. In MySQL Workbench or the MySQL shell:

```sql
CREATE DATABASE IF NOT EXISTS banking_db;
```

On startup, Hibernate updates the tables (`spring.jpa.hibernate.ddl-auto=update`). You do not need to create the tables by hand.

## Configure the app

Open `src/main/resources/application.properties`. The database URL is already set:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/banking_db
```

Set `spring.datasource.username` and `spring.datasource.password` to the MySQL account on your machine. Keep that password out of Git.

The server port is `8081`.

## Install dependencies and run

From the project folder:

```powershell
.\mvnw.cmd spring-boot:run
```

The first run downloads the project dependencies. Later runs reuse them.

If Maven is already installed, this does the same thing:

```powershell
mvn spring-boot:run
```

## Check that it started

The log should include a line like:

```text
Started BankingSystemApplication
```

Tomcat is then listening on `http://localhost:8081`.

Stop the app with `Ctrl+C` in that terminal.
