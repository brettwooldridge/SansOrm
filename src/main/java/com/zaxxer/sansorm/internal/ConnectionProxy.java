/*
 Copyright 2015, Brett Wooldridge

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
