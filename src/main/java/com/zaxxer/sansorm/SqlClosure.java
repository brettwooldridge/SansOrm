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

package com.zaxxer.sansorm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.sansorm.internal.ConnectionProxy;
import com.zaxxer.sansorm.transaction.TransactionElf;

/**
 * The {@code SqlClosure} class provides a convenient way to execute SQL
 * with proper transaction demarcation and resource clean-up.
 *
 * @param <T> the templated return type of the closure
 */
public class SqlClosure<T>
{
   private static DataSource defaultDataSource;
   private Object[] args;
   private DataSource dataSource;

   /**
    * Default constructor using the default DataSource.  The {@code execute(Connection connection)}
    * method will be called when the closure executed.  A RuntimeException is thrown if the default
    * DataSource has not been set.
    */
   public SqlClosure() {
      dataSource = defaultDataSource;
      if (dataSource == null) {
         throw new RuntimeException("No default DataSource has been set");
      }
   }

   /**
    * A constructor taking arguments to be passed to the {@code execute(Connection connection, Object...args)}
    * method when the closure is executed.  Subclasses using this method must call {@code super(args)}.
    * A RuntimeException is thrown if the default DataSource has not been set.
    *
    * @param args arguments to be passed to the execute method
    */
   public SqlClosure(final Object... args) {
      this.args = args;
   }

   /**
    * Construct a SqlClosure with a specific DataSource.
    *
    * @param ds the DataSource
    */
   public SqlClosure(final DataSource ds) {
      dataSource = ds;
   }

   /**
    * Construct a SqlClosure with a specific DataSource and arguments to be passed to the
    * {@code execute} method.  @see #SqlClosure(Object...args)
    *
    * @param ds the DataSource
    * @param args optional arguments to be used for execution
    */
   public SqlClosure(final DataSource ds, final Object... args) {
      this.dataSource = ds;
      this.args = args;
   }

   /**
    * Construct a SqlClosure with the same DataSource as the closure passed in.
    *
    * @param copyClosure the SqlClosure to share a common DataSource with
    */
   public SqlClosure(final SqlClosure copyClosure)
   {
      this.dataSource = copyClosure.dataSource;
   }

   /**
    * Set the default DataSource used by the SqlClosure when the default constructor
    * is used.
    *
    * @param ds the DataSource to use by the default
    */
   static void setDefaultDataSource(final DataSource ds)
   {
      defaultDataSource = ds;
   }

   /**
    * Execute a lambda {@code SqlFunction} closure.
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    */
   public static <V> V sqlExecute(final SqlFunction<V> functional)
   {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure.
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    */
   public static <V> V sqlExecute(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * Execute a lambda {@code SqlFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    */
   public final <V> V exec(final SqlFunction<V> functional)
   {
      return new SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    */
   public final <V> V exec(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * Execute the closure.
    *
    * @return the template return type of the closure
    */
   public final T execute()
   {
      boolean txOwner = !TransactionElf.hasTransactionManager() || TransactionElf.beginOrJoinTransaction();
      Connection connection = null;
      try {
         connection = ConnectionProxy.wrapConnection(dataSource.getConnection());
         if (txOwner) {
            // disable autoCommit mode as we are going to handle transaction by ourselves
            connection.setAutoCommit(false);
         }
         return (args == null)
            ? execute(connection)
            : execute(connection, args);
      }
      catch (SQLException e) {
         if (e.getNextException() != null) {
            e = e.getNextException();
         }
         if (txOwner) {
            // set the txOwner to false as we no longer own the transaction and we shouldn't try to commit it later
            txOwner = false;
            rollback(connection);
         }
         throw new RuntimeException(e);
      } catch (Throwable e) {
         if (txOwner) {
            txOwner = false;
            rollback(connection);
         }
         throw e;
      }
      finally {
         try {
            if (txOwner) {
               commit(connection);
            }
         }
         finally {
            quietClose(connection);
         }
      }
   }

   /**
    * Execute the closure with the specified arguments.  Note using this method
    * does not create a true closure because the arguments are not encapsulated
    * within the closure itself.  Meaning you cannot create an instance of the
    * closure and pass it to another executor.
    *
    * @param args arguments to be passed to the {@code execute(Connection connection, Object...args)} method
    * @return the result of the execution
    */
   public final T executeWith(Object... args)
   {
      this.args = args;
      return execute();
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection, Object...args)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   protected T execute(final Connection connection) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @param args the arguments passed into the {@code SqlClosure(Object...args)} constructor
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   protected T execute(final Connection connection, Object... args) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * @param connection The database connection
    */
   public static void quietClose(final Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @param statement The database connection
    */
   public static void quietClose(final Statement statement)
   {
      if (statement != null) {
         try {
            statement.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @param resultSet The database connection
    */
   public static void quietClose(final ResultSet resultSet)
   {
      if (resultSet != null) {
         try {
            resultSet.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   private static void rollback(final Connection connection)
   {
      if (TransactionElf.hasTransactionManager()) {
         TransactionElf.rollback();
      }
      else if (connection != null) {
         try {
            connection.rollback();
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }

   private static void commit(final Connection connection)
   {
      if (TransactionElf.hasTransactionManager()) {
         TransactionElf.commit();
      }
      else if (connection != null) {
         try {
            connection.commit();
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }
}
