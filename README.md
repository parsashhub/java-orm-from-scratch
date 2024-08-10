# Simple JPA-like ORM in Java

## Overview

This project demonstrates a simple Java-based ORM (Object-Relational Mapping) system, similar to JPA (Java Persistence
API). The goal is to provide a basic understanding of how ORM systems work by creating and managing database tables and
records using custom annotations and reflection.

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

## Project Structure

```
src/
│
├── org/example/
│   ├── Column.java
│   ├── Customer.java
│   ├── Entity.java
│   ├── EntityManager.java
│   ├── Id.java
│   └── Main.java
└── README.md
```

### File Descriptions

- **`Entity.java`**: Defines the `@Entity` annotation to mark classes as database entities.
- **`Column.java`**: Defines the `@Column` annotation to map fields to database columns.
- **`Id.java`**: Defines the `@Id` annotation to mark fields as primary keys.
- **`Customer.java`**: Example entity class that represents a customer in the database.
- **`EntityManager.java`**: Core class that provides methods to interact with the database using the annotated entities.
- **`Main.java`**: Entry point of the application where database operations are performed.

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

### Example Output

When you run the `Main` class, it will:

- Create a `customer` table if it doesn't exist.
- Add an `email` column to the `customer` table.
- Insert a new customer record.
- Update the customer's details.
- Retrieve and display the updated customer's information.

Example console output:

```
Connected to the MySQL server successfully.
Found customer: Jane Doe
```

## License

This project is open-source and available under the MIT License.

---
