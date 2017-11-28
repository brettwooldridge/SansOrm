package com.zaxxer.sansorm;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.zaxxer.sansorm.transaction.TransactionElf;
import com.zaxxer.sansorm.transaction.TxTransactionManager;

/** Single point of SansOrm configuration */
public final class SansOrm {
   private SansOrm() {
   }

   /** Use this one if you don't need {@link TransactionManager} tx handling. */
   public static DataSource initializeTxNone(DataSource dataSource) {
      SqlClosure.setDefaultDataSource(dataSource);
      return dataSource;
   }

   /** Use this one to use simple embedded {@link TransactionManager} implementation for tx handling. */
   public static DataSource initializeTxSimple(DataSource dataSource) {
      TxTransactionManager txManager = new TxTransactionManager(dataSource);
      return initializeTxCustom(txManager.getTxDataSource(), txManager, txManager);
   }

   /** Use this one if you have custom/provided {@link TransactionManager}, e.g. to run within web app container. */
   public static DataSource initializeTxCustom(DataSource dataSource, TransactionManager txManager, UserTransaction userTx) {
      TransactionElf.setTransactionManager(txManager);
      TransactionElf.setUserTransaction(userTx);
      return initializeTxNone(dataSource);
   }
}
