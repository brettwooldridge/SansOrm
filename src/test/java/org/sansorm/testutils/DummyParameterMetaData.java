package org.sansorm.testutils;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.03.18
 */
public class DummyParameterMetaData implements ParameterMetaData {
   @Override
   public int getParameterCount() throws SQLException {
      return 0;
   }

   @Override
   public int isNullable(int param) throws SQLException {
      return 0;
   }

   @Override
   public boolean isSigned(int param) throws SQLException {
      return false;
   }

   @Override
   public int getPrecision(int param) throws SQLException {
      return 0;
   }

   @Override
   public int getScale(int param) throws SQLException {
      return 0;
   }

   @Override
   public int getParameterType(int param) throws SQLException {
      return 0;
   }

   @Override
   public String getParameterTypeName(int param) throws SQLException {
      return null;
   }

   @Override
   public String getParameterClassName(int param) throws SQLException {
      return null;
   }

   @Override
   public int getParameterMode(int param) throws SQLException {
      return 0;
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
   }
}
