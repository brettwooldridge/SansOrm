package com.zaxxer.sansorm.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;


public class ConnectionProxy implements InvocationHandler
{
   private final ArrayList<Statement> statements;
   private final Connection delegate;

   private ConnectionProxy(Connection delegate)
   {
      this.delegate = delegate;
      this.statements = new ArrayList<>();
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if ("close".equals(method.getName())) {
         try {
            for (Statement stmt : statements) {
               stmt.close();
            }
         }
         finally {
            statements.clear();
         }
      }

      final Object ret = method.invoke(delegate, args);
      if (ret instanceof Statement) {
         statements.add((Statement) ret);
      }

      return ret;
   }

   public static Connection wrapConnection(final Connection delegate) {
      ConnectionProxy handler = new ConnectionProxy(delegate);
      return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(), new Class[] { Connection.class }, handler);
   }
}
