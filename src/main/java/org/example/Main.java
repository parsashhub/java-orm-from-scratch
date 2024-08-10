package org.example;


import java.sql.*;

public class Main {
    public static void main(String[] args) {
        var dataSource = new CustomDataSource();
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            EntityManager<Customer> customerEntityManager = new EntityManager<>(conn);
            EntityManager<User> userEntityManager = new EntityManager<>(conn);

            // create a user table
            userEntityManager.createTable(User.class);

            customerEntityManager.createTable(Customer.class);
            // alter table
            // dbSchema.addColumn(Customer.class, "email", "VARCHAR(255)");

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
                // Set a savepoint before making changes
                Savepoint savepoint = entityManager.setSavepoint("MySavepoint");

                try {
                    Customer customer = new Customer();
                    customer.setFirstName("john");
                    customer.setLastName("doe");
                    entityManager.insert(customer);

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
            Customer foundCustomer = customerEntityManager.find(Customer.class, 1);
            if (foundCustomer != null)
                System.out.println("found customer: " + foundCustomer.getFirstName() + " " + foundCustomer.getLastName());

            // update an existing customer
            if (foundCustomer != null) {
                foundCustomer.setFirstName("Jane");
                customerEntityManager.update(foundCustomer);
            }

            // select and print all records
            customerEntityManager.selectAndPrintAll(Customer.class);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // return the connection to the pool
            if (conn != null)
                dataSource.returnConnection(conn);
        }
    }
}