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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
     * @return The object or <code>null</code>
     */
    public static <T> T getObjectById(Class<T> type, Object... ids)
    {
        return new ObjectByIdClosure<T>(type, ids).execute();
    }

    /**
     * Gets an object using a from clause.
     * @param type The type of the desired object.
     * @param clause The WHERE clause.
     * @param args The arguments for the WHERE clause.
     * @param <T> The type of the object.
     * @return The object or <code>null</code>
     */
    public static <T> T objectFromClause(Class<T> type, String clause, Object... args)
    {
        return new ObjectFromClause<T>(type, clause, args).execute();
    }

    /**
     * Inserts the given object into the database.
     * @param object The object to insert.
     * @param <T> The type of the object.
     * @return The inserted object populated with any generated IDs.
     */
    public static <T> T insertObject(T object)
    {
        return new InsertClosure<T>(object).execute();
    }

    /**
     * Updates the given object in the database.
     * @param object The object to update.
     * @param <T> The type of the object.
     * @return The updated object.
     */
    public static <T> T updateObject(T object)
    {
        return new UpdateClosure<T>(object).execute();
    }

    /**
     * Delete the given object in the database.
     * @param object the object to delete.
     * @param <T> The type of the object.
     * @return the number of rows affected.
     */
    public static <T> int deleteObject(T object)
    {
        return new DeleteClosure<T>(object).execute();
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
        return new DeleteByIdClosure<T>(clazz, args).execute();
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
        return new ListFromClauseClosure<T>(clazz, clause, args).execute();
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
        return new CountObjectsFromClause<T>(clazz, clause, args).execute();
    }

    /**
     * Executes an update or insert statement.
     * @param sql The SQL to execute.
     * @param args The query parameters used
     */
    public static int executeUpdate(final String sql, final Object... args)
    {
        return new SqlClosure<Integer>()
        {
            @Override
            protected Integer execute(Connection connection) throws SQLException
            {
                return OrmElf.executeUpdate(connection, sql, args);
            }
        }.execute();
    }

    /**
     * listFromClause
     */
    private static class ListFromClauseClosure<T> extends SqlClosure<List<T>>
    {
        private Class<T> clazz;
        private String clause;
        private Object[] args;

        public ListFromClauseClosure(Class<T> clazz, String clause, Object[] args)
        {
            this.clazz = clazz;
            this.clause = clause;
            this.args = args;
        }

        @Override
        protected List<T> execute(Connection connection) throws SQLException
        {
            return OrmElf.listFromClause(connection, clazz, clause, args);
        }
    }

    /**
     * updateObject
     */
    private static class UpdateClosure<T> extends SqlClosure<T>
    {
        private T object;

        public UpdateClosure(T object)
        {
            this.object = object;
        }

        @Override
        protected T execute(Connection connection) throws SQLException
        {
            return OrmElf.updateObject(connection, object);
        }
    }

    /**
     * insertObject
     */
    private static class InsertClosure<T> extends SqlClosure<T>
    {
        private T object;

        public InsertClosure(T object)
        {
            this.object = object;
        }

        @Override
        protected T execute(Connection connection) throws SQLException
        {
            return OrmElf.insertObject(connection, object);
        }
    }

    /**
     * deleteObject
     */
    private static class DeleteClosure<T> extends SqlClosure<Integer>
    {
        private T object;

        public DeleteClosure(T object)
        {
            this.object = object;
        }

        @Override
        protected Integer execute(Connection connection) throws SQLException
        {
            return OrmElf.deleteObject(connection, object);
        }
    }

    /**
     * deleteObject
     */
    private static class DeleteByIdClosure<T> extends SqlClosure<Integer>
    {
        private Class<T> clazz;
        private Object[] args;

        public DeleteByIdClosure(Class<T> clazz, Object... args)
        {
            this.clazz = clazz;
            this.args = args;
        }

        @Override
        protected Integer execute(Connection connection) throws SQLException
        {
            return OrmElf.deleteObjectById(connection, clazz, args);
        }
    }

    /**
     * @see OrmElf#objectFromClause(Connection, Class, String, Object...)
     */
    private static class ObjectFromClause<T> extends SqlClosure<T>
    {
        private Class<T> clazz;
        private String clause;
        private Object[] args;

        public ObjectFromClause(Class<T> clazz, String clause, Object[] args)
        {
            this.clause = clause;
            this.args = args;
            this.clazz = clazz;
        }

        @Override
        protected T execute(Connection connection) throws SQLException
        {
            return OrmElf.objectFromClause(connection, clazz, clause, args);
        }
    }

    /**
     * objectById
     */
    private static class ObjectByIdClosure<T> extends SqlClosure<T>
    {
        private Class<T> type;
        private Object[] ids;

        public ObjectByIdClosure(Class<T> type, Object[] ids)
        {
            this.type = type;
            this.ids = ids;
        }

        @Override
        protected T execute(Connection connection) throws SQLException
        {
            return OrmElf.objectById(connection, type, ids);
        }
    }

    /**
     * wraps {@link OrmElf#countObjectsFromClause(Connection, Class, String, Object...)}
     */
    private static class CountObjectsFromClause<T> extends SqlClosure<Integer>
    {
        private Class<T> clazz;
        private String clause;
        private Object[] args;

        public CountObjectsFromClause(Class<T> clazz, String clause, Object[] args)
        {
            this.clazz = clazz;
            this.clause = clause;
            this.args = args;
        }

        @Override
        protected Integer execute(Connection connection) throws SQLException
        {
            return OrmElf.countObjectsFromClause(connection, clazz, clause, args);
        }
    }

    /**
     * @param connection The database connection
     */
    public static void quietClose(Connection connection)
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                return;
            }
        }
    }

    /**
     * @param statement The database connection
     */
    public static void quietClose(Statement statement)
    {
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (SQLException e)
            {
                return;
            }
        }
    }

    /**
     * @param resultSet The database connection
     */
    public static void quietClose(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (SQLException e)
            {
                return;
            }
        }
    }
}
