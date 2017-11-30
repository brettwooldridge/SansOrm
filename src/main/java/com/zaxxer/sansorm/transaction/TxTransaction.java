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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

public class TxTransaction implements Transaction
{
   private final ArrayList<Synchronization> synchronizations;
   private volatile Connection connection;
   private volatile int status;

   TxTransaction() {
      status = Status.STATUS_NO_TRANSACTION;
      synchronizations = new ArrayList<>();
   }

   // ------------------------------------------------------------------------
   //                     Transaction Interface Methods
   // ------------------------------------------------------------------------

   @Override
   public int getStatus() throws SystemException
   {
      return status;
   }

   @Override
   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      if (status == Status.STATUS_NO_TRANSACTION) {
         throw new IllegalStateException("transaction hasn't started yet");
      }
      else if (isDone()) {
         throw new IllegalStateException("transaction is done, cannot commit it");
      }

      if (connection != null && status == Status.STATUS_ACTIVE) {
         try {
               synchronizations.forEach(Synchronization::beforeCompletion);
               connection.commit();
               synchronizations.forEach(s -> s.afterCompletion(Status.STATUS_COMMITTED));
         }
         catch (SQLException e) {
            final SystemException systemException = new SystemException("Exception committing connection " + connection.toString());
            systemException.initCause(e);
            throw systemException;
         }
         finally {
            cleanup();
         }
      }
   }

   @Override
   public void rollback() throws IllegalStateException, SystemException
   {
      if (connection != null && status == Status.STATUS_ACTIVE) {
         try {
            connection.rollback();
         }
         catch (SQLException e) {
            final SystemException systemException = new SystemException("Exception committing connection " + connection.toString());
            systemException.initCause(e);
            throw systemException;
         }
         finally {
            cleanup();
         }
      }
   }

   @Override
   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      throw new SystemException("setRollbackOnly() operation is not supported");
   }

   @Override
   public boolean delistResource(final XAResource xaRes, final int flag) throws IllegalStateException, SystemException
   {
      throw new SystemException("delistResource() operation is not supported");
   }

   @Override
   public boolean enlistResource(final XAResource xaRes) throws RollbackException, IllegalStateException, SystemException
   {
      throw new SystemException("enlistResource() operation is not supported");
   }

   @Override
   public void registerSynchronization(final Synchronization sync) throws RollbackException, IllegalStateException, SystemException
   {
      synchronizations.add(sync);
   }

   // ------------------------------------------------------------------------
   //                             Local Methods
   // ------------------------------------------------------------------------

   void setActive()
   {
      if (status != Status.STATUS_NO_TRANSACTION) {
         throw new IllegalStateException("transaction has already started");
      }

      status = Status.STATUS_ACTIVE;
   }

   Connection getConnection()
   {
      return connection;
   }

   void setConnection(final Connection connection)
   {
      this.connection = connection;
   }

   private void cleanup()
   {
      try {
         connection.close();
      }
      catch (SQLException e) {
         // ignore
      }

      synchronizations.clear();
      status = Status.STATUS_NO_TRANSACTION;
   }

   /**
    * See https://github.com/bitronix/btm/blob/1072c3042c8b65ecf17ded88115631e061f23333/btm/src/main/java/bitronix/tm/BitronixTransaction.java#L580
    * for reference.
    *
    * @return whether the current transaction is done or not
    */
   private boolean isDone()
   {
      switch (status) {
      case Status.STATUS_PREPARING:
      case Status.STATUS_PREPARED:
      case Status.STATUS_COMMITTING:
      case Status.STATUS_COMMITTED:
      case Status.STATUS_ROLLING_BACK:
      case Status.STATUS_ROLLEDBACK:
         return true;
      default:
         return false;
      }
   }
}
