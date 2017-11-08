/*
 Copyright 2017, Brett Wooldridge

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

import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public class TxTransactionManager implements TransactionManager, UserTransaction
{
   private final DataSource dataSource;

   public TxTransactionManager(final DataSource dataSource)
   {
      this.dataSource = TxDataSource.getWrappedDataSource(dataSource);
   }

   @Override
   public void begin() throws NotSupportedException, SystemException
   {
      final TxThreadContext context = TxThreadContext.getThreadContext();
      if (context.getTransaction() != null) {
         throw new NotSupportedException("Nested transactions not supported");
      }

      final TxTransaction newTransaction = new TxTransaction();
      newTransaction.setActive();
      context.setTransaction(newTransaction);
   }

   @Override
   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      final TxThreadContext threadContext = TxThreadContext.getThreadContext();

      final TxTransaction currentTx = threadContext.getTransaction();
      if (currentTx != null) {
         threadContext.clearTransaction();
         currentTx.commit();
      }
      else {
         throw new IllegalStateException("TransactionManager.commit() called from a thread that never joined a transaction");
      }
   }

   @Override
   public int getStatus() throws SystemException
   {
      final TxTransaction transaction = TxThreadContext.getThreadContext().getTransaction();
      return (transaction != null) ? transaction.getStatus() : Status.STATUS_NO_TRANSACTION;
   }

   @Override
   public Transaction getTransaction() throws SystemException
   {
      return TxThreadContext.getThreadContext().getTransaction();
   }

   @Override
   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      final TxThreadContext threadContext = TxThreadContext.getThreadContext();

      final TxTransaction currentTx = threadContext.getTransaction();
      if (currentTx != null) {
         threadContext.clearTransaction();
         currentTx.rollback();
      }
      else {
         throw new IllegalStateException("TransactionManager.rollback() called from a thread that never joined a transaction");
      }

   }

   @Override
   public Transaction suspend() throws SystemException
   {
      throw new SystemException("suspend() operation is not supported");
   }

   @Override
   public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException
   {
      throw new SystemException("resume() operation is not supported");
   }

   @Override
   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      throw new SystemException("setRollbackOnly() operation is not supported");
   }

   @Override
   public void setTransactionTimeout(final int seconds) throws SystemException
   {
      throw new SystemException("setTransactionTimeout() operation is not supported");
   }

   public DataSource getTxDataSource()
   {
      return dataSource;
   }
}
