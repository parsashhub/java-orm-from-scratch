package org.example;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<T> {
    private final Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    public void insert(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new RuntimeException("not an entity class");

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

        for (var field : fields)
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                sql.append(column.name()).append(",");
                field.setAccessible(true);
                System.out.println(column.name() + ": " + field.get(entity));
                values.add(field.get(entity));
            }

        sql.setLength(sql.length() - 1); // to remove the additional ","
        sql.append(") VALUES (");

        for (int i = 0; i < values.size(); i++)
            sql.append("'").append(values.get(i)).append("'").append(",");

        sql.setLength(sql.length() - 1);
        sql.append(")");
        System.out.println("...\ngenerated sql: " + sql);
        // we can use preparedStatement too:
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql.toString());
        }
        System.out.println("...\ninserted " + entityClass.getSimpleName() + " into " + tableName + " table successfully:)");
    }

    public T find(Class<T> entityClass, int primaryKey) throws Exception {
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new RuntimeException("not an entity class");

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        var fields = entityClass.getDeclaredFields();
        Field idField = null;
        System.out.println("\nsearching for a " + entityClass.getSimpleName() +
                " with id:" + primaryKey + " in " + tableName + " table");
        System.out.println("fields: " + Arrays.stream(fields)
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::name).collect(Collectors.joining(", ")) + "\n...");

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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
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
                    return entity;
                }
            }
        }
        return null;
    }

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

        sql.setLength(sql.length() - 1);
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
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i));
            }
            preparedStatement.executeUpdate();
        }
        System.out.println("...\nupdated " + entityClass.getSimpleName() + " in " + tableName + " table successfully:)");
    }

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
                    // this is just for simplification, we can add more types
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

        sql.setLength(sql.length() - 1);  // remove additional comma
        sql.append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
        }
        System.out.println("...\ntable " + entityClass.getSimpleName() + " created successfully:)");
    }

    public void addColumn(Class<T> entityClass, String columnName, String columnType) throws Exception {
        System.out.println("\naltering table " + entityClass.getSimpleName());
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException("not an entity class");
        }

        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();

        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        System.out.println("...\ntable " + entityClass.getSimpleName() + " altered successfully:)");
    }
}
