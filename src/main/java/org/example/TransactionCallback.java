package org.example;


// define a contract for executing database operations within a transaction
interface TransactionCallback<T> {
    void doInTransaction(EntityManager<T> entityManager) throws Exception;
}