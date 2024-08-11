# Simple JPA-like ORM in Java

## Overview

This project demonstrates a simple Java-based ORM (Object-Relational Mapping) system, similar to JPA (Java Persistence API), with a basic caching mechanism. The goal is to provide a basic understanding of how ORM systems work by creating and managing database tables and records using custom annotations, reflection, and a simple caching layer to optimize performance.

## simplified version of a basic ORM-like system in java (jpa & jdbc):

1. define entity classes: entity classes with annotations to map them to database tables.
2. create an EntityManager: implement an EntityManager class to handle persistence operations.
3. create a Configuration Class: manage database connections and configurations.
4. implement CRUD Operations: implement basic CRUD operations using jdbc under the hood.

## annotations:

1. **@Retention**:
   specifies how long annotations with this type are to be retained. It has a single value which is one of the constants
   from the java.lang.annotation.RetentionPolicy enum

- RetentionPolicy.SOURCE: annotations are discarded by the compiler.
- RetentionPolicy.CLASS: annotations are recorded in the class file but are not retained by the JVM at runtime.(default
  behavior)
- RetentionPolicy.RUNTIME: annotations are recorded in the class file and retained by the JVM at runtime, so they can be
  read reflectively.

2. **@Target**:
   specifies the kinds of program elements to which an annotation type is applicable. It has a single value which is an
   array of java.lang.annotation.ElementType constants.

- ElementType.TYPE: applicable to any element of a class (e.g., class, interface, enum).
- ElementType.FIELD: applicable to fields or properties.
- ElementType.METHOD: applicable to methods.
- ElementType.PARAMETER: applicable to the parameters of a method.
- ElementType.CONSTRUCTOR: applicable to constructors.
- ElementType.LOCAL_VARIABLE: applicable to local variables.


## Features

- **Entity Mapping**: Annotate Java classes to map them to database tables.
- **Field Mapping**: Annotate fields to map them to database columns.
- **Caching Mechanism**: Implement a simple first-level cache (session-level) to reduce database access and improve performance.
- **CRUD Operations**:
    - Create new records (`insert`)
    - Read records by primary key (`find`)
    - Update existing records (`update`)
- **Table Management**:
    - Create new tables (`createTable`)
    - Alter existing tables by adding new columns (`addColumn`)
- **Transaction Management**:
    - Basic transaction handling
    - Support for transactions with savepoints

---

## Hibernate Caching Overview

In Hibernate, caching is an essential feature for improving the performance of database operations. Hibernate provides two levels of caching:

1. **First-Level Cache (Session Cache)**:
    - Built-in and enabled by default.
    - Operates at the session level, meaning it's specific to the Hibernate session and not shared between sessions.
    - Ensures that within a single session, the same entity instance is not loaded more than once, which reduces database access.

2. **Second-Level Cache**:
    - Optional and needs to be configured explicitly.
    - Operates at the session factory level, meaning it can be shared across multiple sessions.
    - Stores entity data in a cache that survives beyond the session, reducing the need to go back to the database for read-only data.

### Custom Caching Implementation

In this project, we have implemented a basic form of first-level caching in our custom `EntityManager` class. The goal is to reduce the number of database queries by caching entity instances within the scope of a single session.

### How Caching Works in This ORM

1. **Session-Level Cache**:
    - Each `EntityManager` instance maintains a cache (e.g., a `Map`) that stores entities by their primary key.
    - When an entity is requested using the `find` method, the cache is checked first. If the entity is found in the cache, it is returned directly without querying the database.
    - If the entity is not in the cache, it is retrieved from the database and stored in the cache for future requests.

2. **Cache Eviction**:
    - When an entity is updated using the `update` method, the cache is updated to reflect the changes. This ensures that subsequent requests for the entity return the latest data.
    - Entities are removed from the cache when the `EntityManager` is closed or the session ends, mimicking the behavior of a Hibernate session.

### Benefits of Caching

- **Reduced Database Access**: By caching entities, the system reduces the need to query the database multiple times for the same data within a session, improving performance.
- **Improved Performance**: Caching reduces latency by avoiding unnecessary database calls, making the system more efficient.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: Make sure you have JDK installed.
- **MySQL Database**: The example assumes a MySQL database running on `localhost:3306` with a database named `test`.
- you can install mysql with the following command using docker:

```bash 
docker container run -d -p 3306:3306 --name mySql -e MYSQL_ROOT_PASSWORD=123456 mysql
```

### Setup

1. **Clone the repository**:
    ```bash
    git clone https://github.com/parsashhub/java-orm-from-scratch.git
    cd java-orm-from-scratch
    ```

2. **Configure MySQL Database**:
    - Ensure you have a MySQL database named `test`.
    - Update the `applications.properties` file with your MySQL credentials if needed:
        ```
        db.url=jdbc:mysql://localhost:3306/test
        db.username=root
        db.password=123456
        db.driver=com.mysql.cj.jdbc.Driver
         ```

3. **Compile and Run**:
    - Compile the project:
        ```bash
        javac -d bin src/org/example/*.java
        ```
    - Run the project:
        ```bash
        java -cp bin org.example.Main
        ```

### Usage

The `Main` class demonstrates basic usage of the `EntityManager`:

- **Creating a Table**: Automatically create a table for the `Customer` entity.
- **Adding a Column**: Add a new column to the existing table.
- **Persisting a Record**: Insert a new customer record into the database.
- **Updating a Record**: Update an existing customer's details.
- **Finding a Record**: Retrieve a customer record by its primary key.
- **Caching**: Demonstrates the use of caching by showing how the `find` method checks the cache before querying the database.

## project structure
```
src/
│
├── org/example/
│   ├── Cache.java
│   ├── Column.java
│   ├── CustomCache.java
│   ├── CustomDataSource.java
│   ├── Customer.java
│   ├── Entity.java
│   ├── EntityManager.java
│   ├── Id.java
│   ├── TransactionCallback.java
│   ├── User.java
│   └── Main.java
└── README.md
```

### Example Output

When you run the `Main` class, it will:

- Create a `customer` table if it doesn't exist.
- Add an `email` column to the `customer` table.
- Insert a new customer record.
- Update the customer's details.
- Retrieve and display the updated customer's information.
- Showcase caching by retrieving an entity from the cache without a database query.

Example console output:

```
Connected to the MySQL server successfully.
Found customer: Jane Doe
```

---

## License

This project is open-source and available under the MIT License.

---