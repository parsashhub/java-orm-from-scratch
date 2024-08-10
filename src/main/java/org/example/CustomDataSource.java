package org.example;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Logger;

public class CustomDataSource implements DataSource {
    private String url;
    private String username;
    private String password;
    private String driver;
    private PrintWriter logWriter;
    private int loginTimeout;
    private LinkedList<Connection> connectionPool;
    private int maxPoolSize = 5;

    public CustomDataSource() {
        loadProperties();
        // ensuring that the JDBC driver class is loaded into the Java application’s memory.
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load JDBC driver", e);
        }
        initializeConnectionPool();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/applications.properties")) {
            props.load(fis);
            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");
            this.driver = props.getProperty("db.driver");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeConnectionPool() {
        connectionPool = new LinkedList<>();
        for (int i = 0; i < maxPoolSize; i++) {
            connectionPool.add(createNewConnection());
        }
    }

    private Connection createNewConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating a new database connection", e);
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (connectionPool.isEmpty()) {
            if (connectionPool.size() < maxPoolSize)
                connectionPool.add(createNewConnection());
            else
                throw new SQLException("Maximum pool size reached, no available connections!");

        }
        // retrieves and removes the head (the first element) of the LinkedList,
        // returning null if the LinkedList is empty
        return connectionPool.poll();
    }

    @Override
    public synchronized Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public synchronized void returnConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                // adds the specified element to the LinkedList.
                // returns true if the element was added successfully,
                // and false if the element could not be added.
                // though in the case of LinkedList, offer() always returns true because the list can grow dynamically
                connectionPool.offer(connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    // wrapper (unwrap, isWrapperFor): handle flexible type handling.
    // check if the DataSource can be unwrapped to a particular class.
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return iface.cast(this);
        else
            throw new SQLException("The DataSource is not a wrapper for " + iface.getName());

    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}
