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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * SqlClosure
 *
 * @param <T> the templated return type of the closure
 */
public abstract class SqlClosure<T>
{
    private static DataSource defaultDataSource;

    private List<Statement> closeStatements;
    private List<ResultSet> closeResultSets;

    private DataSource dataSource;

    // Instance initializer
    {
        closeStatements = new ArrayList<Statement>();
        closeResultSets = new ArrayList<ResultSet>();
    }

    /**
     * Default constructor.  Uses the default DataSource.  A RuntimeException is
     * thrown if the default DataSource has not been set.
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
     * Construct a SqlClosure with a specific DataSource.
     *
     * @param ds the DataSource
     */
    public SqlClosure(DataSource ds)
    {
        dataSource = ds;
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

            return execute(connection);
        }
        catch (SQLException e)
        {
            if (e.getNextException() != null)
            {
                e = e.getNextException();
            }

            if (owner)
            {
                TransactionElf.rollback();
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

            SqlClosureElf.quietClose(connection);

            if (owner)
            {
                TransactionElf.commit();
            }
        }
    }

    protected final Statement autoClose(Statement statement)
    {
        if (statement != null)
        {
            closeStatements.add(statement);
        }
        return statement;
    }

    protected final PreparedStatement autoClose(PreparedStatement statement)
    {
        if (statement != null)
        {
            closeStatements.add(statement);
        }
        return statement;
    }

    protected final ResultSet autoClose(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            closeResultSets.add(resultSet);
        }
        return resultSet;
    }

    protected abstract T execute(final Connection connection) throws SQLException;
}
