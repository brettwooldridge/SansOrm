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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import javax.sql.DataSource;
import javax.transaction.Status;

class TxDataSource implements InvocationHandler
{
   private final DataSource delegate;

   private TxDataSource(final DataSource delegate)
   {
      this.delegate = delegate;
   }

   static DataSource getWrappedDataSource(final DataSource dataSource)
   {
    TxDataSource handler = new TxDataSource(dataSource);
      return (DataSource) Proxy.newProxyInstance(TxDataSource.class.getClassLoader(), new Class[] { DataSource.class }, handler);
   }

   @Override
   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      if ("getConnection".equals(method.getName())) {
         final TxThreadContext context = TxThreadContext.getThreadContext();

         final TxTransaction transaction = context.getTransaction();
         if (transaction != null && transaction.getConnection() != null && transaction.getStatus() == Status.STATUS_ACTIVE)
         {
            return transaction.getConnection();
         }
         else
         {
            final Connection wrappedConnection = ConnectionProxy.getWrappedConnection(delegate.getConnection());
            if (transaction != null) {
               transaction.setConnection(wrappedConnection);
            }

            return wrappedConnection;
         }
      }

      return method.invoke(delegate, args);
   }

   static class ConnectionProxy implements InvocationHandler
   {
      private final Connection delegate;

      private ConnectionProxy(final Connection delegate)
      {
         this.delegate = delegate;
      }

      static Connection getWrappedConnection(final Connection delegate) {
         final ConnectionProxy handler = new ConnectionProxy(delegate);
         return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(), new Class[] { Connection.class }, handler);
      }

      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
      {
        final TxTransaction transaction = TxThreadContext.getThreadContext().getTransaction();
         if (transaction != null) {
            switch (method.getName())
            {
            case "close":
               // ignore close() of a connection during a transaction
               return null;
            case "commit":
               throw new IllegalStateException("Calling Connection.commit() is not legal during a transaction.");
            case "rollback":
               throw new IllegalStateException("Calling Connection.commit() is not legal during a transaction.");
            }
         }

         return method.invoke(delegate, args);
      }
   }
}
