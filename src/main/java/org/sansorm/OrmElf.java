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

package org.sansorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
* OrmElf
*/
//CHECKSTYLE:OFF
public final class OrmElf
{
    /**
     * Private constructor.
     */
    private OrmElf()
    {
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
     * @throws SQLException thrown if a SQLException occurs
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
     * <code>
     *    User user = OrmElf.objectFromClause(connection, User.class, "username=?", userName);
     * </code>
     * or<p>
     * <code>
     *    User user = 
     * </code>
     *
     * @param connection a SQL Connection object
     * @param clazz the class of the object to load
     * @param clause the conditional part of a SQL where clause
     * @param args the query parameters used to find the object
     * @param <T> the type of the object to load
     * @return the populated object
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> T objectFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
    {
        return OrmReader.objectFromClause(connection, clazz, clause, args);
    }

    /**
     * Load a list of objects using the specified where condition.  The clause "WHERE" is automatically
     * appended, so the <code>where</code> parameter should just be the conditional portion.
     *
     * If the <code>where</code> parameter is <code>null</code> a select of every object from the
     * table mapped for the specified class is executed.
     *
     * @param connection a SQL Connection object
     * @param clazz the class of the object to load
     * @param clause the conditional part of a SQL where clause
     * @param args the query parameters used to find the list of objects
     * @param <T> the type of the object to load
     * @return a list of populated objects
     * @throws SQLException thrown if a SQLException occurs
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

    public static int countFromSql(Connection connection, String sql, Object... args) throws SQLException
    {
        return OrmReader.numberFromSql(connection, sql, args).intValue();
    }

    public static Number numberFromSql(Connection connection, String sql, Object... args) throws SQLException
    {
        return OrmReader.numberFromSql(connection, sql, args);
    }

    /**
     * This method takes a PreparedStatement, a target class, and optional arguments to set
     * as query parameters.  It sets the parameters automatically, executes the query, and
     * constructs and populates an instance of the target class.
     *
     * The PreparedStatement will closed.
     * 
     * @param stmt the PreparedStatement to execute to construct an object
     * @param clazz the class of the object to instantiate and populate with state
     * @param args optional arguments to set as query parameters in the PreparedStatement
     * @param <T> the class template
     * @return the populated object
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> T statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
    {
        return OrmReader.statementToObject(stmt, clazz, args);
    }

    /**
     * Execute a prepared statement (query) with the supplied args set as query parameters (if specified), and
     * return a list of objects as a result.
     *
     * The PreparedStatement will closed.
     * 
     * @param stmt the PreparedStatement to execute
     * @param clazz the class of the objects to instantiate and populate with state
     * @param args optional arguments to set as query parameters in the PreparedStatement
     * @param <T> the class template
     * @return a list of instance of the target class, or an empty list
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> List<T> statementToList(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
    {
        return OrmReader.statementToList(stmt, clazz, args);
    }

    /**
     * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
     * this should be done by the caller.  The ResultSet is not closed as a result of this
     * method.
     *
     * @param resultSet the SQL ResultSet
     * @param target the target object to set values on
     * @param <T> the class template
     * @return the populated object
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> T resultSetToObject(ResultSet resultSet, T target) throws SQLException
    {
        return OrmReader.resultSetToObject(resultSet, target);
    }

    /**
     * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
     * this should be done by the caller.  The ResultSet is not closed as a result of this
     * method.
     *
     * @param resultSet the SQL ResultSet
     * @param target the target object to set values on
     * @param ignoredColumns the columns in the result set to ignore.
     * @param <T> the class template
     * @return the populated object
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> T resultSetToObject(ResultSet resultSet, T target, Set<String> ignoredColumns) throws SQLException
    {
        return OrmReader.resultSetToObject(resultSet, target, ignoredColumns);
    }

    /**
     * This method will iterate over a ResultSet that contains columns that map to the
     * target class and return a list of target instances.  Note, this assumes that 
     * ResultSet.next() has <i>NOT</i> been called before calling this method.
     *
     * The entire ResultSet will be consumed and closed.
     *
     * @param resultSet the ResultSet
     * @param targetClass the target class
     * @return a list of instance of the target class, or an empty list
     * @throws SQLException thrown if a SQLException occurs
     */
    public static <T> List<T> resultSetToList(ResultSet resultSet, Class<T> targetClass) throws SQLException
    {
        return OrmReader.resultSetToList(resultSet, targetClass);
    }

    // ------------------------------------------------------------------------
    //                               Write Methods
    // ------------------------------------------------------------------------

    public static int executeUpdate(Connection connection, String sql, Object... args) throws SQLException
    {
        return OrmWriter.executeUpdate(connection, sql, args);
    }

    public static <T> void insertListNotBatched(Connection connection, Iterable<T> iterable) throws SQLException
    {
        OrmWriter.insertListNotBatched(connection, iterable);
    }

    public static <T> void insertListBatched(Connection connection, Iterable<T> iterable) throws SQLException
    {
        OrmWriter.insertListBatched(connection, iterable);
    }

    public static <T> T insertObject(Connection connection, T target) throws SQLException
    {
        return OrmWriter.insertObject(connection, target);
    }

    public static <T> T updateObject(Connection connection, T target) throws SQLException
    {
        return OrmWriter.updateObject(connection, target);
    }

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

    public static <T> String getColumnsCsv(Class<T> clazz, String... tablePrefix)
    {
        return OrmReader.getColumnsCsv(clazz, tablePrefix);
    }
}
