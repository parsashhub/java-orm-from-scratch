package org.example;


import java.sql.*;

public class Main {
    public static void main(String[] args) {
        var dataSource = new CustomDataSource();
        Cache<Integer, Customer> customCache = new CustomCache<>();
        try {
            EntityManager<Customer> customerEntityManager = new EntityManager<>(dataSource, customCache);
            EntityManager<User> userEntityManager = new EntityManager<>(dataSource, null);

            // create a user table
            userEntityManager.createTable(User.class);

            customerEntityManager.createTable(Customer.class);
            // alter table
            // dbSchema.addColumn(Customer.class, "email", "VARCHAR(255)");

            // select and print all records
            customerEntityManager.selectAndPrintAll(Customer.class);

            // transaction
            customerEntityManager.executeInTransaction(entityManager -> {
                Customer customer = new Customer();
                customer.setFirstName("transaction");
                customer.setLastName("customer");
                entityManager.insert(customer);

                Customer existingCustomer = entityManager.find(Customer.class, 1);
                if (existingCustomer != null) {
                    existingCustomer.setLastName("Smith");
                    entityManager.update(existingCustomer);
                }
            });

            // transaction using save points
            customerEntityManager.executeInTransactionWithSavepoints(entityManager -> {
                Savepoint savepoint = null;
                try {
                    Customer customer = new Customer();
                    customer.setFirstName("john");
                    customer.setLastName("doe");
                    entityManager.insert(customer);

                    // Set a savepoint after adding customer
                    savepoint = entityManager.setSavepoint("MySavepoint");

                    Customer existingCustomer = entityManager.find(Customer.class, 1);
                    if (existingCustomer != null) {
                        existingCustomer.setLastName("Smith");
                        entityManager.update(existingCustomer);
                    }
                    entityManager.releaseSavepoint(savepoint);

                    // simulate an error to roll back to the savepoint
                    // throw new RuntimeException("Simulating an error");
                } catch (Exception e) {
                    // roll back to the savepoint if an error occurs
                    entityManager.rollback(savepoint);
                    System.out.println("rolled back to savepoint due to error: " + e.getMessage());
                }
            });

            // insert a new customer
            Customer customer = new Customer();
            customer.setFirstName("parsa");
            customer.setLastName("sh");
            customerEntityManager.insert(customer);

            // find by id
            Customer foundCustomer = customerEntityManager.find(Customer.class, 4);
            if (foundCustomer != null)
                System.out.println("found customer: " + foundCustomer.getFirstName() + " " + foundCustomer.getLastName());
            // this will be return from cache
            Customer checkCache = customerEntityManager.find(Customer.class, 4);

            // update an existing customer
            if (foundCustomer != null) {
                foundCustomer.setFirstName("Jane");
                customerEntityManager.update(foundCustomer);
            }

             // select and print all records
             customerEntityManager.selectAndPrintAll(Customer.class);


            // Shutdown hook to close all connections when the application ends
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    dataSource.closeAllConnections();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}