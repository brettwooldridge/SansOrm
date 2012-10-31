## Preface

SansOrm is a "No-ORM" sane Java-to-SQL/SQL-to-Java object mapping library.  It was created to rid my company's 
product of Hibernate.  However, even if you do "pure JDBC", you will find SansOrm extrmemly useful.  After about
10 years of using ORMs in various projects, I came to the same conclusion as others: [ORM is an Anti-Pattern](https://github.com/brettwooldridge/SansOrm/wiki/ORM-is-an-anti-pattern).

TL;DR:

*  Standard ORMs do not scale.
*  Don't fear the SQL.

Either you buy into it, or you don't.  If you do, keep reading.

## SansOrm

SansOrm is not an ORM.  SansOrm library will...

* Massively decrease the boilerplate code you write even if you use pure SQL (and no Java objects)
* Persist and retrieve simple annotated Java objects, and lists thereof, _without you writing SQL_
* Persist and retrieve simple annotated Java objects, and lists thereof, _where you provide the SQL_

SansOrm will _never_...

* Perform a JOIN for you
* Persist a nested hierarchy of objects for you
* Lazily retrieve anything for you
* Page data for you

These things that SansOrm will _never_ do are better and more efficiently performed by _you_.  SansOrm will _help_ you do them simply, but there is no magic under the covers.

### Basic SQL

We'll work from simle to complex.  In the first examples, the savings in code will not seem that great, but as we go through the examples you'll notice the code using SansOrm vs. pure Java/JDBC gets gets more and more compact.

SansOrm provides you with two important classes.  Let's look at the first, which has nothing to do with Java objects or persistence.  This class just makes your life easier when writing raw SQL (JDBC).  It is called _SqlClosure_.

Typical Java pure JDBC with correct resource cleanup:
```Java
    public int getUserCount(String usernameWildcard) {
       Connection connection = null;
       try {
          connection = dataSource.getConnection();
          PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username LIKE ?");
          stmt.setString(1, usernameWildcard);

          int count = 0;
          ResultSet resultSet = stmt.executeQuery();
          if (resultSet.next() {
             count = resultSet.getInt(1);
          }
          resultSet.close();
          stmt.close();
          return count;
       }
       finally {
          if (connection != null) {
             try {
                connection.close();
             }
             catch (SQLException e) {
                // ignore
             }
          }
       }
    }
```

Now the same code using SansOrm's SqlClosure:
```Java
    public int getUserCount(final String usernameWildcard) {
       return new SqlClosure<Integer>() {
          public Integer execute(Connection connection) {
              PreparedStatement stmt = autoClose(connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username LIKE ?"));
              stmt.setString(1, usernameWildcard);
              ResultSet resultSet = autoClose(stmt.executeQuery());
              if (resultSet.next()) {
                 return resultSet.getInt(1);
              }
              return 0;
          }
       }.execute();
    }
```
Important points:
* The SqlClosure class is a generic (templated) class
* The SqlClosure class will call your ```execute()``` method with a provided connection
   * The provided connection will be closed quietly automatically (i.e. exceptions in ```connection.close()``` will be eaten)
* The SqlClosure class offers an ```autoClose()``` method for Statements (/PreparedStatements) and ResultSets
   * The resource passed to ```autoClose()``` will be closed quietly automatically
* SqlExceptions thrown from the body if the ```execute()``` method will be wrapped in a RuntimeException

As mentioned above, the SqlClosure class is generic, and the signature looks something like this:
```Java
    public class T SqlClosure<T> {
       public abstract T execute(Connection);
       public T execute() { ... }
    }
```
SqlClosure is typically constructed as an anonymous class, and you must provide the implementation of 
the ```execute(Connection connection)``` method.  Invoking the ```execute()``` method (no parameters) will create a
Connection and invoke your overridden method, cleaning up resources in a finally, and returning the value
returned by the overridden method.

Let's look at an example of returning a complex type:
```Java
    public Set<String> getAllUsernames() {
       return new SqlClosure<Set<String>>() {
          public Set<String> execute(Connection connection) {
             Set<String> usernames = new HashSet<>();
             Statement statement = autoClose(connection.createStatement());
             ResultSet resultSet = autoClose(statement.executeQuery("SELECT username FROM users"));
             while (resultSet.next()) {
                usernames.add(resultSet.getString("username"));
             }
             return usernames;
          }
       }.execute();
    }
```

### Object Mapping
While the SqlClosure is extremly useful and helps reduce the boilerplate code that you write, we know why you're here:
_object mapping_.  Let's jump right in with some examples.

Take these database tables:
```SQL
    CREATE TABLE customer (
       customer_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
       last_name VARCHAR(255),
       first_name VARCHAR(255),
       email VARCHAR(255)
    );

    CREATE TABLE order (
       order_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
       customer_num INTEGER NOT NULL
    );
```
