package com.zaxxer.sansorm;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T>
{
   T execute(Connection connection) throws SQLException;
}
