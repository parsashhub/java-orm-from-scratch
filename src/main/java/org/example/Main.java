package org.example;


import java.sql.*;

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/test";
        try (Connection conn = DriverManager.getConnection(url, "root", "123456")) {
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