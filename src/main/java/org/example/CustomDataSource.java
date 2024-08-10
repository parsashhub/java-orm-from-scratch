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
    private int maxPoolSize = 5; // Maximum number of connections in the pool

    public CustomDataSource() {
        loadProperties(); // Load database connection properties
        // Ensure the JDBC driver class is loaded
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load JDBC driver", e);
        }
        initializeConnectionPool(); // Initialize the connection pool
    }

    // Load database connection properties from a properties file
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

    // Initialize the connection pool with a set number of connections
    private void initializeConnectionPool() {
        connectionPool = new LinkedList<>();
        for (int i = 0; i < maxPoolSize; i++) {
            connectionPool.add(createNewConnection()); // Create and add new connections to the pool
        }
    }

    // Create a new database connection
    private Connection createNewConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating a new database connection", e);
        }
    }

    // Get a connection from the pool
    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (connectionPool.isEmpty()) {
            if (connectionPool.size() < maxPoolSize) {
                // Add a new connection if the pool size is below the maximum
                connectionPool.add(createNewConnection());
            } else {
                throw new SQLException("Maximum pool size reached, no available connections!");
            }
        }
        // Retrieve and remove the head (the first element) of the LinkedList
        return connectionPool.poll();
    }

    // Get a connection with specific username and password
    @Override
    public synchronized Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // Return a connection back to the pool
    public synchronized void returnConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                // Add the connection back to the pool
                connectionPool.offer(connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get the log writer for this DataSource
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    // Set the log writer for this DataSource
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    // Set the login timeout value
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    // Get the login timeout value
    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    // Get the parent logger for this DataSource
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null; // SQLFeatureNotSupportedException is thrown if not supported
    }

    // Check if this DataSource can be unwrapped to a specific class
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this); // Return this DataSource cast to the specified class
        } else {
            throw new SQLException("The DataSource is not a wrapper for " + iface.getName());
        }
    }

    // Check if this DataSource is a wrapper for a specific class
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}
