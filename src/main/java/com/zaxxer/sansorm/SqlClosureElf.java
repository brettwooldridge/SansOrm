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
import java.util.List;

import com.zaxxer.sansorm.internal.OrmReader;
import com.zaxxer.sansorm.internal.OrmWriter;

/**
 * Provides SQL closures around common query types
 */
public final class SqlClosureElf
{
    private SqlClosureElf()
    {
    }

    /**
     * Gets an object by ID from the database.
     * @param type The type of the desired object.
     * @param ids The ID or IDs of the object.
     * @param <T> The type of the object.
     * @return The object or {@code null}
     */
    public static <T> T getObjectById(Class<T> type, Object... ids)
    {
        return SqlClosure.sqlExecute(c -> OrmElf.objectById(c, type, ids));
    }

    /**
     * Gets an object using a from clause.
     * @param type The type of the desired object.
     * @param clause The WHERE clause.
     * @param args The arguments for the WHERE clause.
     * @param <T> The type of the object.
     * @return The object or {@code null}
     */
    public static <T> T objectFromClause(Class<T> type, String clause, Object... args)
    {
        return SqlClosure.sqlExecute(c -> OrmElf.objectFromClause(c, type, clause, args));
    }

    /**
     * Inserts the given object into the database.
     * @param object The object to insert.
     * @param <T> The type of the object.
     * @return The inserted object populated with any generated IDs.
     */
    public static <T> T insertObject(T object)
    {
        return SqlClosure.sqlExecute(c -> OrmElf.insertObject(c, object));
    }

    /**
     * Updates the given object in the database.
     * @param object The object to update.
     * @param <T> The type of the object.
     * @return The updated object.
     */
    public static <T> T updateObject(T object)
    {
        return SqlClosure.sqlExecute(c -> OrmElf.updateObject(c, object));
    }

    /**
     * Delete the given object in the database.
     * @param object the object to delete.
     * @param <T> The type of the object.
     * @return the number of rows affected.
     */
    public static <T> int deleteObject(T object)
    {
        return SqlClosure.sqlExecute(c ->  OrmElf.deleteObject(c, object));
    }

    /**
     * Delete an object from the database by ID.
     * @param clazz the class of the object to delete.
     * @param args the IDs of the object, in order of appearance of declaration in the target object class.
     * @param <T> The type of the object.
     * @return the number of rows affected.
     */
    public static <T> int deleteObjectById(Class<T> clazz, Object... args)
    {
       return SqlClosure.sqlExecute(c -> OrmElf.deleteObjectById(c, clazz, args));
    }

    /**
     * Gets a list of objects from the database.
     * @param clazz The type of the desired objects.
     * @param clause The from or where clause.
     * @param args The arguments needed for the clause.
     * @param <T> The type of the objects.
     * @return The list of objects.
     */
    public static <T> List<T> listFromClause(Class<T> clazz, String clause, Object... args)
    {
       return SqlClosure.sqlExecute(c -> OrmElf.listFromClause(c, clazz, clause, args));
    }

    /**
     * Counts the number of rows for the given query.
     *
     * @param clazz the class of the object to query.
     * @param clause The conditional part of a SQL where clause.
     * @param args The query parameters used to find the list of objects.
     * @param <T> the type of object to query.
     * @return The result count.
     */
    public static <T> int countObjectsFromClause(Class<T> clazz, String clause, Object... args)
    {
        return SqlClosure.sqlExecute(c -> OrmElf.countObjectsFromClause(c, clazz, clause, args));
    }

    /**
     * Get a single Number from a SQL query, useful for getting a COUNT(), SUM(), MIN/MAX(), etc.
     * from a SQL statement.  If the SQL query is parametrized, the parameter values can
     * be passed in as arguments following the {@code sql} String parameter.
     *
     * @param sql a SQL statement string
     * @param args optional values for a parametrized query
     * @return the resulting number or {@code null}
     */
    public static Number numberFromSql(String sql, Object... args)
    {
        return SqlClosure.sqlExecute(c -> numberFromSql(c, sql, args));
    }

    /**
     * Executes an update or insert statement.
     * @param sql The SQL to execute.
     * @param args The query parameters used
     * @return the number of rows updated
     */
    public static int executeUpdate(final String sql, final Object... args)
    {
       return SqlClosure.sqlExecute(c -> executeUpdate(c, sql, args));
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
      // items.length of "?" + items.length-1 of "," + 2 spaces + 2 brackets
      final StringBuilder sb = new StringBuilder(3 + placeholderCount * 2);
      sb.append(" (?");
      for (int i = 1; i < placeholderCount; i++)
      {
         sb.append(",?");
      }
      return sb.append(") ").toString();
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
}
