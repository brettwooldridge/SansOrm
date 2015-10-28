package com.zaxxer.sansorm;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlClosureFuncInterface<T>
{
   T execute(Connection connection) throws SQLException;
}
