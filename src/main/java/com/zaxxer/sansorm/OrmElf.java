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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.zaxxer.sansorm.internal.Introspector;
import com.zaxxer.sansorm.internal.OrmReader;
import com.zaxxer.sansorm.internal.OrmWriter;

/**
* OrmElf
*/
//CHECKSTYLE:OFF
public final class OrmElf
{
   /**
    * Private constructor.
    */
   private OrmElf() {
      // private constructor
   }

   // ------------------------------------------------------------------------
   //                               Read Methods
   // ------------------------------------------------------------------------

   /**
    * Load an object by it's ID.  The @Id annotated field(s) of the object is used to
    * set query parameters.
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param args the query parameter used to find the object by it's ID
    * @param <T> the type of the object to load
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T objectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.objectById(connection, clazz, args);
   }

   /**
    * Load an object using the specified clause.  If the specified clause contains the text
    * "WHERE" or "JOIN", the clause is appended directly to the generated "SELECT .. FROM" SQL.
    * However, if the clause contains neither "WHERE" nor "JOIN", it is assumed to be
    * just be the conditional portion that would normally appear after the "WHERE", and therefore
    * the clause "WHERE" is automatically appended to the generated "SELECT .. FROM" SQL, followed
    * by the specified clause.  For example:<p>
    * {@code User user = OrmElf.objectFromClause(connection, User.class, "username=?", userName);}
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the object
    * @param <T> the type of the object to load
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T objectFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.objectFromClause(connection, clazz, clause, args);
   }

   /**
    * Load a list of objects using the specified where condition.  The clause "WHERE" is automatically
    * appended, so the {@code where} parameter should just be the conditional portion.
    *
    * If the {@code where} parameter is <code>null</code> a select of every object from the
    * table mapped for the specified class is executed.
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the list of objects
    * @param <T> the type of the object to load
    * @return a list of populated objects
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> listFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromClause(connection, clazz, clause, args);
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param connection a SQL connection object.
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int countObjectsFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.countObjectsFromClause(connection, clazz, clause, args);
   }

   /**
    * Get a single Number from a SQL query, useful for getting a COUNT(), SUM(), MIN/MAX(), etc.
    * from a SQL statement.  If the SQL query is parameterized, the parameter values can
    * be passed in as arguments following the {@code sql} String parameter.
    *
    * @param connection a SQL connection object.
    * @param sql a SQL statement string
    * @param args optional values for a parameterized query
    * @return the resulting number or {@code null}
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static Number numberFromSql(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmReader.numberFromSql(connection, sql, args);
   }

   /**
    * This method takes a PreparedStatement, a target class, and optional arguments to set
    * as query parameters.  It sets the parameters automatically, executes the query, and
    * constructs and populates an instance of the target class.  <b>The PreparedStatement will closed.</b>
    *
    * @param stmt the PreparedStatement to execute to construct an object
    * @param clazz the class of the object to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToObject(stmt, clazz, args);
   }

   /**
    * Execute a prepared statement (query) with the supplied args set as query parameters (if specified), and
    * return a list of objects as a result.  <b>The PreparedStatement will closed.</b>
    *
    * @param stmt the PreparedStatement to execute
    * @param clazz the class of the objects to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> statementToList(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToList(stmt, clazz, args);
   }

   /**
    * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
    * this should be done by the caller.  <b>The ResultSet is not closed as a result of this
    * method.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param target the target object to set values on
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T resultSetToObject(ResultSet resultSet, T target) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target);
   }

   /**
    * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
    * this should be done by the caller.  <b>The ResultSet is not closed as a result of this
    * method.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param target the target object to set values on
    * @param ignoredColumns the columns in the result set to ignore.
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T resultSetToObject(ResultSet resultSet, T target, Set<String> ignoredColumns) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target, ignoredColumns);
   }

   /**
    * This method will iterate over a ResultSet that contains columns that map to the
    * target class and return a list of target instances.  <b>Note, this assumes that
    * ResultSet.next() has <i>NOT</i> been called before calling this method.</b>
    * <p>
    * <b>The entire ResultSet will be consumed and closed.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param targetClass the target class
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> resultSetToList(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      return OrmReader.resultSetToList(resultSet, targetClass);
   }

   // ------------------------------------------------------------------------
   //                               Write Methods
   // ------------------------------------------------------------------------

   /**
    * Insert a collection of objects in a non-batched manner (i.e. using iteration and individual INSERTs).
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertListNotBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListNotBatched(connection, iterable);
   }

   /**
    * Insert a collection of objects using JDBC batching.
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertListBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListBatched(connection, iterable);
   }

   /**
    * Insert an annotated object into the database.
    *
    * @param connection a SQL connection
    * @param target the annotated object to insert
    * @param <T> the class template
    * @return the same object that was passed in, but with possibly updated @Id field due to auto-generated keys
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T insertObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.insertObject(connection, target);
   }

   /**
    * Update a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated UPDATE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to update a row in the database
    * @param <T> the class template
    * @return the same object passed in
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T updateObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.updateObject(connection, target);
   }

   /**
    * Delete a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated DELETE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to delete a row in the database
    * @param <T> the class template
    * @return 0 if no row was deleted, 1 if the row was deleted
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int deleteObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.deleteObject(connection, target);
   }

   public static <T> int deleteObjectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmWriter.deleteObjectById(connection, clazz, args);
   }

   // ------------------------------------------------------------------------
   //                             Utility Methods
   // ------------------------------------------------------------------------

   /**
    * Execute the specified SQL as a PreparedStatement with the specified arguments.
    *
    * @param connection a Connection
    * @param sql the SQL statement to prepare and execute
    * @param args the optional arguments to execute with the query
    * @return a ResultSet object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static ResultSet executeQuery(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmReader.statementToResultSet(connection.prepareStatement(sql), args);
   }

   public static int executeUpdate(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmWriter.executeUpdate(connection, sql, args);
   }

   /**
    * Gets the column name defined for the given property for the given type.
    *
    * @param clazz The type.
    * @param propertyName The object property name.
    * @return The database column name.
    */
   public static String getColumnFromProperty(Class<?> clazz, String propertyName)
   {
      return Introspector.getIntrospected(clazz).getColumnNameForProperty(propertyName);
   }

   /**
    * Get a comma separated values list of column names for the given class, suitable
    * for inclusion into a SQL SELECT statement.
    *
    * @param clazz the annotated class
    * @param tablePrefix an optional table prefix to append to each column
    * @param <T> the class template
    * @return a CSV of annotated column names
    */
   public static <T> String getColumnsCsv(Class<T> clazz, String... tablePrefix)
   {
      return OrmReader.getColumnsCsv(clazz, tablePrefix);
   }

   /**
    * Get a comma separated values list of column names for the given class -- <i>excluding
    * the column names specified</i>, suitable for inclusion into a SQL SELECT statement.
    * Note the excluded column names must exactly match annotated column names in the class
    * in a case-sensitive manner.
    *
    * @param clazz the annotated class
    * @param excludeColumns optional columns to exclude from the returned list of columns
    * @param <T> the class template
    * @return a CSV of annotated column names
    */
   public static <T> String getColumnsCsvExclude(Class<T> clazz, String... excludeColumns)
   {
      return OrmReader.getColumnsCsvExclude(clazz, excludeColumns);
   }

   /**
    * Get a SQL "IN" clause for the number of items.
    * Provided as a conventient alternative to {@link #getInClausePlaceholdersForCount(int)}
    * (at a cost of possible additional array construction).
    *
    * @param <T> to ensure that all items are on the same type
    * @param items a list of items
    * @return a parenthetical String with {@code item.length} placeholders, eg. " (?,?,?,?) ".
    */
   @SafeVarargs
   public static <T> String getInClausePlaceholders(final T... items)
   {
      return getInClausePlaceholdersForCount(items.length);
   }

   /**
    * Get a SQL "IN" clause for the number of items.
    *
    * @param placeholderCount a count of "?" placeholders
    * @return a parenthetical String with {@code item.length} placeholders, eg. " (?,?,?,?) ".
    * @throws IllegalArgumentException if placeholderCount is negative
    */
   public static String getInClausePlaceholdersForCount(final int placeholderCount)
   {
      // we cant overload method name because the only item for getInClausePlaceholders can be Integer which leads to ambiguity
      if (placeholderCount < 0)
      {
         throw new IllegalArgumentException("Placeholder count must be greater than or equal to zero");
      }
      if (placeholderCount == 0)
      {
         return " ('s0me n0n-ex1st4nt v4luu') ";
      }
      // items.lengh of "?" + items.length-1 of "," + 2 spaces + 2 brackets
      final StringBuilder sb = new StringBuilder(3 + placeholderCount * 2);
      sb.append(" (?");
      for (int i = 1; i < placeholderCount; i++)
      {
         sb.append(",?");
      }
      return sb.append(") ").toString();
   }
}
