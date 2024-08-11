package org.example;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<T> {
    private CustomDataSource dataSource;
    private Cache<Integer, T> cache;

    public EntityManager(CustomDataSource dataSource, Cache<Integer, T> cache) {
        this.dataSource = dataSource;
        this.cache = cache;
    }

    public void setCache(Cache<Integer, T> cache) {
        this.cache = cache;
    }

    // Insert a new entity into the database
    public void insert(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();
        // Check if the class has the Entity annotation
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new RuntimeException("not an entity class");

        // Get table name and fields from entity class
        var entityAnnotation = entityClass.getAnnotation(Entity.class);
        var tableName = entityAnnotation.tableName();
        var fields = entityClass.getDeclaredFields();

        System.out.println("\ninserting " + entityClass.getSimpleName() + " into " + tableName + " table");
        System.out.println("fields: " + Arrays.stream(fields)
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::name).collect(Collectors.joining(", ")));

        var sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        List<Object> values = new ArrayList<>();
        System.out.println("...\nprovided values and columns:");

        for (var field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                sql.append(column.name()).append(",");
                field.setAccessible(true);
                System.out.println(column.name() + ": " + field.get(entity));
                values.add(field.get(entity));
            }
        }

        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(") VALUES (");

        for (int i = 0; i < values.size(); i++)
            sql.append("'").append(values.get(i)).append("'").append(",");

        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(")");
        System.out.println("...\ngenerated sql: " + sql);

        // Execute the SQL INSERT statement
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql.toString());

            System.out.println("...\ninserted " + entityClass.getSimpleName() + " into " + tableName + " table successfully:)");
            dataSource.returnConnection(connection);
        }
    }

    // Find an entity by its primary key
    public T find(Class<T> entityClass, int primaryKey) throws Exception {
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new RuntimeException("not an entity class");

        // Check the cache first
        T cachedEntity = cache.get(primaryKey);
        if (cachedEntity != null) {
            System.out.println("Entity found in cache");
            return cachedEntity;
        }

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        var fields = entityClass.getDeclaredFields();
        Field idField = null;

        System.out.println("\nsearching for a " + entityClass.getSimpleName() +
                " with id:" + primaryKey + " in " + tableName + " table");
        System.out.println("fields: " + Arrays.stream(fields)
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::name).collect(Collectors.joining(", ")) + "\n...");

        // Find the ID field
        for (var field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
                break;
            }
        }

        if (idField == null) {
            throw new RuntimeException("no Id field found");
        }

        Column idColumn = idField.getAnnotation(Column.class);
        String idColumnName = idColumn.name();
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?";

        System.out.println("generated sql: " + sql);

        // Execute the SQL SELECT statement
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, primaryKey);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    T entity = entityClass.getDeclaredConstructor().newInstance();
                    for (var field : fields) {
                        if (field.isAnnotationPresent(Column.class)) {
                            Column column = field.getAnnotation(Column.class);
                            field.setAccessible(true);
                            field.set(entity, rs.getObject(column.name()));
                        }
                    }
                    // Put the fetched entity in the cache
                    cache.put(primaryKey, entity);

                    return entity;
                }
            }
            dataSource.returnConnection(connection);
        }
        return null;
    }

    // Update an existing entity in the database
    public void update(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("not an entity class");
        }

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        var fields = entityClass.getDeclaredFields();

        System.out.println("\nupdating " + entityClass.getSimpleName() + " " + tableName + " table");
        System.out.println("fields: " + Arrays.stream(fields)
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::name).collect(Collectors.joining(", ")));

        List<Object> values = new ArrayList<>();
        Object primaryKeyValue = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    primaryKeyValue = field.get(entity);
                } else {
                    sql.append(column.name()).append(" = ?,");
                    values.add(field.get(entity));
                }
            }
        }

        if (primaryKeyValue == null) {
            throw new RuntimeException("Primary key not found");
        }

        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(" WHERE ");

        for (var field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                Column column = field.getAnnotation(Column.class);
                sql.append(column.name()).append(" = ?");
                values.add(primaryKeyValue);
                break;
            }
        }
        System.out.println("generated sql: " + sql);

        // Execute the SQL UPDATE statement
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i));
            }
            preparedStatement.executeUpdate();
            System.out.println("...\nupdated " + entityClass.getSimpleName() + " in " + tableName + " table successfully:)");

            // Update the cache with the new entity data
            cache.put((Integer) primaryKeyValue, entity);
            dataSource.returnConnection(connection);
        }
    }

    // Create a table for the entity class if it does not exist
    public void createTable(Class<T> entityClass) throws Exception {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("not an entity class");
        }
        System.out.println("\ncreating table " + entityClass.getSimpleName() + " if not exists!");
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                sql.append(column.name()).append(" ");
                if (field.isAnnotationPresent(Id.class)) {
                    sql.append("INT PRIMARY KEY AUTO_INCREMENT,");
                } else {
                    // Simplified type mapping
                    if (field.getType() == String.class) {
                        sql.append("VARCHAR(255),");
                    } else if (field.getType() == int.class) {
                        sql.append("INT,");
                    } else {
                        throw new RuntimeException("Unsupported field type");
                    }
                }
            }
        }

        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(")");

        // Execute the SQL CREATE TABLE statement
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
            System.out.println("...\ntable " + entityClass.getSimpleName() + " created successfully:)");
            dataSource.returnConnection(connection);
        }
    }

    // Add a new column to an existing table
    public void addColumn(Class<T> entityClass, String columnName, String columnType) throws Exception {
        System.out.println("\naltering table " + entityClass.getSimpleName());
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("not an entity class");
        }

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();

        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;

        // Execute the SQL ALTER TABLE statement
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("...\ntable " + entityClass.getSimpleName() + " altered successfully:)");
            dataSource.returnConnection(connection);
        }
    }

    // Select and print all records from the table corresponding to the entity class
    public void selectAndPrintAll(Class<T> entityClass) throws Exception {
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new RuntimeException("not an entity class");

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        String sql = "SELECT * FROM " + tableName;

        System.out.println("\nSelecting and printing all records from " + tableName + " table");

        // Execute the SQL SELECT statement and print the results
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            // Print column names
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();

            // Print each row
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
            dataSource.returnConnection(connection);
        }
    }

    // Execute a transaction with auto-commit disabled
    public void executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false); // Start transaction
            callback.doInTransaction(this); // Execute callback
            connection.commit(); // Commit transaction
            System.out.println("Transaction committed successfully");
        } catch (Exception e) {
            try {
                connection.rollback(); // Rollback transaction on error
                System.out.println("Transaction rolled back due to error: " + e.getMessage());
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true); // Reset auto-commit to true
                dataSource.returnConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Execute a transaction with savepoints to handle partial rollbacks
    public void executeInTransactionWithSavepoints(TransactionCallback<T> callback) throws SQLException {
        Savepoint savepoint = null;
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false); // Start transaction

            callback.doInTransaction(this);

            connection.commit(); // Commit transaction
            System.out.println("Transaction committed successfully");

        } catch (Exception e) {
            try {
                if (savepoint != null) {
                    connection.rollback(savepoint); // Rollback to savepoint on error
                    System.out.println("Rolled back to savepoint due to error: " + e.getMessage());
                } else {
                    connection.rollback(); // Rollback transaction if no savepoint
                    System.out.println("Transaction rolled back due to error: " + e.getMessage());
                }
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true); // Reset auto-commit to true
                dataSource.returnConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Set a savepoint in the transaction
    public Savepoint setSavepoint(String savepointName) throws SQLException {
        Savepoint savepoint = dataSource.getConnection().setSavepoint(savepointName);
        System.out.println("savepoint '" + savepointName + "' set.");
        return savepoint;
    }

    // Release a savepoint (it cannot be rolled back to once released)
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        dataSource.getConnection().releaseSavepoint(savepoint);
        System.out.println("savepoint released.");
    }

    // Rollback to a specific savepoint
    public void rollback(Savepoint savepoint) throws SQLException {
        dataSource.getConnection().rollback(savepoint);
    }
}
