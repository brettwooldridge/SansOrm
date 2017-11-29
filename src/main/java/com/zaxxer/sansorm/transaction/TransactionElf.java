/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.sansorm.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public final class TransactionElf
{
   private static final Logger LOGGER = LoggerFactory.getLogger(TransactionElf.class);
   private static TransactionManager transactionManager;
   private static UserTransaction userTransaction;

   private TransactionElf() {
   }

   /**
    * Set the JTA TransactionManager implementation used by the Elf.
    *
    * @param tm a JTA TransactionManager instance
    */
   public static void setTransactionManager(TransactionManager tm)
   {
      transactionManager = tm;
   }

   public static void setUserTransaction(UserTransaction ut)
   {
      userTransaction = ut;
   }

   /**
    * Returns true if a JTA transaction manager is registered, false
    * otherwise.
    *
    * @return true if a JTA transaction manager is registered
    */
   public static boolean hasTransactionManager()
   {
      return transactionManager != null;
   }

   /**
    * Start or join a transaction.
    *
    * @return true if a new transaction was started (this means the caller "owns"
    *    the commit()), false if a transaction was joined.
    */
   public static boolean beginOrJoinTransaction()
   {
      boolean newTransaction = false;
      try {
         newTransaction = userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION;
         if (newTransaction) {
            userTransaction.begin();
         }
      }
      catch (Exception e) {
         throw new RuntimeException("Unable to start transaction.", e);
      }

      return newTransaction;
   }

   /**
    * Commit the current transaction.
    */
   public static void commit()
   {
      try {
         if (!isDone()) {
            userTransaction.commit();
         }
         else {
            LOGGER.warn("commit() called with no current transaction.");
         }
      }
      catch (Exception e) {
         throw new RuntimeException("Transaction commit failed.", e);
      }
   }

   /**
    * Rollback the current transaction.
    */
   public static void rollback()
   {
      try {
         if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
            userTransaction.rollback();
         }
         else {
            LOGGER.warn("Request to rollback transaction when none was in started.");
         }
      }
      catch (Exception e) {
         LOGGER.warn("Transaction rollback failed.", e);
      }
   }

   /**
    * Suspend the current transaction and return it to the caller.
    *
    * @return the suspended Transaction
    */
   public static Transaction suspend()
   {
      try {
         Transaction suspend = transactionManager.suspend();
         return suspend;
      }
      catch (SystemException e) {
         throw new RuntimeException("Unable to suspend current transaction", e);
      }
   }

   /**
    * Resume the specified transaction.  If the transaction was never suspended, or was
    * already committed or rolled back, a RuntimeException will be thrown wrapping the
    * JTA originated exception.
    *
    * @param transaction the Transaction to resume
    */
   public static void resume(Transaction transaction)
   {
      try {
         transactionManager.resume(transaction);
      }
      catch (Exception e) {
         throw new RuntimeException("Unable to resume transaction", e);
      }
   }

   /**
    * See https://github.com/bitronix/btm/blob/1072c3042c8b65ecf17ded88115631e061f23333/btm/src/main/java/bitronix/tm/BitronixTransaction.java#L580
    * for reference.
    *
    * @return whether the current transaction is done or not
    * @throws SystemException if an exception occurs
    */
   private static boolean isDone() throws SystemException
   {
      switch (userTransaction.getStatus()) {
      case Status.STATUS_PREPARING:
      case Status.STATUS_PREPARED:
      case Status.STATUS_COMMITTING:
      case Status.STATUS_COMMITTED:
      case Status.STATUS_ROLLING_BACK:
      case Status.STATUS_ROLLEDBACK:
         return true;
      }
      return false;
   }
}
