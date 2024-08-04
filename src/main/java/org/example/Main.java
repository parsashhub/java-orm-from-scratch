package org.example;


import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;

public class Main {
    public static void main(String[] args) {
        BasicDataSource dataSource = DataSourceConfig.createDataSource();
        try (Connection conn = dataSource.getConnection()) {
            EntityManager<Customer> dbSchema = new EntityManager<>(conn);

            dbSchema.createTable(Customer.class);
            // Add a new column to the Customer table
            dbSchema.addColumn(Customer.class, "email", "VARCHAR(255)");

            // insert a new customer
            Customer customer = new Customer();
            customer.setFirstName("parsa");
            customer.setLastName("sh");
            dbSchema.insert(customer);

            // find by id
            Customer foundCustomer = dbSchema.find(Customer.class, 1);
            if (foundCustomer != null)
                System.out.println("found customer: " + foundCustomer.getFirstName() + " " + foundCustomer.getLastName());

            // update an existing customer
            if (foundCustomer != null) {
                foundCustomer.setFirstName("Jane");
                dbSchema.update(foundCustomer);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}