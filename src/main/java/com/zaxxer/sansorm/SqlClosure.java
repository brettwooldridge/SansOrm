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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.sansorm.internal.ConnectionProxy;

/**
 * The <code>SqlClosure</code> class provides a convenient way to execute SQL
 * with proper transaction demarcation and resource clean-up. 
 *
 * @param <T> the templated return type of the closure
 */
public class SqlClosure<T> implements SqlFunction<T>, SqlVarArgsFunction<T>
{
   private static DataSource defaultDataSource;

   private List<Statement> closeStatements;
   private List<ResultSet> closeResultSets;

   private Object[] args;

   private DataSource dataSource;

   // Instance initializer
   {
      closeStatements = new ArrayList<Statement>();
      closeResultSets = new ArrayList<ResultSet>();
   }

   /**
    * Default constructor using the default DataSource.  The <code>execute(Connection connection)</code>
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
    * A constructor taking arguments to be passed to the <code>execute(Connection connection, Object...args)</code>
    * method when the closure is executed.  Subclasses using this method must call <code>super(args)</code>.
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
    * <code>execute</code> method.  @see #SqlClosure(Object...args)
    *
    * @param ds the DataSource
    */
   public SqlClosure(final DataSource ds, final Object... args) {
      this.dataSource = ds;
      this.args = args;
   }

   /**
    * Set the default DataSource used by the SqlClosure when the default constructor
    * is used.
    *
    * @param ds the DataSource to use by the default
    */
   public static void setDefaultDataSource(final DataSource ds)
   {
      defaultDataSource = ds;
   }

   public static final <V> V execute(final SqlFunction<V> functional)
   {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   public static final <V> V execute(final SqlVarArgsFunction<V> functional, final Object... args)
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
    * Execute the closure.
    *
    * @return the template return type of the closure
    */
   public final T execute()
   {
      boolean owner = TransactionElf.beginOrJoinTransaction();

      Connection connection = null;
      try {
         connection = ConnectionProxy.wrapConnection(dataSource.getConnection());

         if (args != null) {
            return execute(connection, args);
         }
         else {
            return execute(connection);
         }
      }
      catch (SQLException e) {
         if (e.getNextException() != null) {
            e = e.getNextException();
         }

         if (owner) {
            // set the owner to false as we no longer own the transaction and we shouldn't try to commit it later
            owner = false;

            rollback(connection);
         }

         throw new RuntimeException(e);
      }
      finally {
         for (ResultSet rs : closeResultSets) {
            quietClose(rs);
         }

         for (Statement stmt : closeStatements) {
            quietClose(stmt);
         }

         closeResultSets.clear();
         closeStatements.clear();

         try {
            if (owner) {
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
    * @param args arguments to be passed to the <code>execute(Connection connection, Object...args)</code> method
    * @return
    */
   public final T executeWith(Object... args)
   {
      this.args = args;
      return execute();
   }

   /**
    * Subclasses of <code>SqlClosure</code> must override this method or the alternative
    * <code>execute(Connection connection, Object...args)</code> method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   public T execute(final Connection connection) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * Subclasses of <code>SqlClosure</code> must override this method or the alternative
    * <code>execute(Connection connection)</code> method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @param args the arguments passed into the <code>SqlClosure(Object...args)</code> constructor
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   @Override
   public T execute(final Connection connection, Object... args) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * @param connection The database connection
    */
   public static void quietClose(Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException e) {
            return;
         }
      }
   }

   /**
    * @param statement The database connection
    */
   public static void quietClose(Statement statement)
   {
      if (statement != null) {
         try {
            statement.close();
         }
         catch (SQLException e) {
            return;
         }
      }
   }

   /**
    * @param resultSet The database connection
    */
   public static void quietClose(ResultSet resultSet)
   {
      if (resultSet != null) {
         try {
            resultSet.close();
         }
         catch (SQLException e) {
            return;
         }
      }
   }

   /**
    * Used to automatically close a Statement when the closure completes.
    *
    * @param statement the Statement to automatically close
    * @return the Statement that will be closed (same as the input parameter)
    */
   @Deprecated
   public final <S extends Statement> S autoClose(S statement)
   {
      if (statement != null) {
         closeStatements.add(statement);
      }
      return statement;
   }

   /**
    * Used to automatically code a ResultSet when the closure completes.
    *
    * @param resultSet the ResultSet to automatically close
    * @return the ResultSet that will be closed (same as the input parameter)
    */
   @Deprecated
   public final ResultSet autoClose(ResultSet resultSet)
   {
      if (resultSet != null) {
         closeResultSets.add(resultSet);
      }
      return resultSet;
   }

   private static void rollback(Connection connection)
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

   private static void commit(Connection connection)
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
