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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * The <code>SqlClosure</code> class provides a convenient way to execute SQL
 * with proper transaction demarcation and resource clean-up. 
 *
 * @param <T> the templated return type of the closure
 */
public abstract class SqlClosure<T>
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
    public SqlClosure()
    {
        dataSource = defaultDataSource;
        if (dataSource == null)
        {
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
    public SqlClosure(Object...args)
    {
    	this.args = args;
    }

    /**
     * Construct a SqlClosure with a specific DataSource.
     *
     * @param ds the DataSource
     */
    public SqlClosure(DataSource ds)
    {
        dataSource = ds;
    }

    /**
     * Construct a SqlClosure with a specific DataSource and arguments to be passed to the
     * <code>execute</code> method.  @see #SqlClosure(Object...args)
     *
     * @param ds the DataSource
     */
    public SqlClosure(DataSource ds, Object...args)
    {
        this.dataSource = ds;
        this.args = args;
    }

    /**
     * Set the default DataSource used by the SqlClosure when the default constructor
     * is used.
     *
     * @param ds the DataSource to use by the default
     */
    public static void setDefaultDataSource(DataSource ds)
    {
        defaultDataSource = ds;
    }

    /**
     * Execute the closure.
     *
     * @return the templated return type of the closure
     */
    public final T execute()
    {
        boolean owner = TransactionElf.beginOrJoinTransaction();

        Connection connection = null;
        try
        {
            connection = dataSource.getConnection();

            if (args != null)
            {
            	return execute(connection, args);
            }
            else
            {
            	return execute(connection);
            }
        }
        catch (SQLException e)
        {
            if (e.getNextException() != null)
            {
                e = e.getNextException();
            }

            if (owner)
            {
                rollback(connection);
            }

            throw new RuntimeException(e);
        }
        finally
        {
            for (ResultSet rs : closeResultSets)
            {
                SqlClosureElf.quietClose(rs);
            }

            for (Statement stmt : closeStatements)
            {
                SqlClosureElf.quietClose(stmt);
            }

            closeResultSets.clear();
            closeStatements.clear();

            try
            {
                if (owner)
                {
                    commit(connection);
                }
            }
            finally
            {
                SqlClosureElf.quietClose(connection);
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
    public final T executeWith(Object...args)
    {
    	this.args = args;
    	return execute();
    }

    /**
     * Used to automatically close a Statement when the closure completes.
     *
     * @param statement the Statement to automatically close
     * @return the Statement that will be closed (same as the input parameter)
     */
    protected final <S extends Statement> S autoClose(S statement)
    {
        if (statement != null)
        {
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
    protected final ResultSet autoClose(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            closeResultSets.add(resultSet);
        }
        return resultSet;
    }

    /**
     * Subclasses of <code>SqlClosure</code> must override this method or the alternative
     * <code>execute(Connection connection, Object...args)</code> method.
     * @param connection the Connection to be used, do not close this connection yourself
     * @return the templated return value from the closure
     * @throws SQLException thrown if a SQLException occurs
     */
    protected T execute(final Connection connection) throws SQLException
    {
    	return null;
    }

    /**
     * Subclasses of <code>SqlClosure</code> must override this method or the alternative
     * <code>execute(Connection connection)</code> method.
     * @param connection the Connection to be used, do not close this connection yourself
     * @param args the arguments passed into the <code>SqlClosure(Object...args)</code> constructor
     * @return the templated return value from the closure
     * @throws SQLException thrown if a SQLException occurs
     */
    protected T execute(final Connection connection, Object...args) throws SQLException
    {
    	return null;
    }

    private void rollback(Connection connection)
    {
        if (TransactionElf.hasTransactionManager())
        {
            TransactionElf.rollback();
        }
        else if (connection != null)
        {
            try
            {
                connection.rollback();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void commit(Connection connection)
    {
        if (TransactionElf.hasTransactionManager())
        {
            TransactionElf.commit();
        }
        else if (connection != null)
        {
            try
            {
                connection.commit();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
