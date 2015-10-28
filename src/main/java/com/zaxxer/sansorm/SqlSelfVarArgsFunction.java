package com.zaxxer.sansorm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface SqlSelfVarArgsFunction<T>
{
   T execute(SqlSelfVarArgsFunction<T> _this, Connection connection, Object... args) throws SQLException;

   default ResultSet autoClose(ResultSet resultSet)
   {
      throw new AbstractMethodError("You are not meant to override this method.");
   }

   public default <S extends Statement> S autoClose(S statement)
   {
      throw new AbstractMethodError("You are not meant to override this method.");
   }
}
