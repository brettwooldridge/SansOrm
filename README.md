## Preface

SansOrm is a "No-ORM" sane Java-to-SQL/SQL-to-Java object mapping library.  It was created to rid my company's 
product of Hibernate.  However, even if you do "pure JDBC", you will find SansOrm extrmemly useful.  After about
10 years of using ORMs in various projects, I came to the same conclusion as others: [ORM is an Anti-Pattern](https://github.com/brettwooldridge/SansOrm/wiki/ORM-is-an-anti-pattern).

TL;DR:

*  Standard ORMs do not scale.
*  Don't fear the SQL.
*  What are you, lazy?  Read the page.

## SansOrm

SansOrm is not an ORM.  SansOrm library will...

* Massively decrease the boilerplate code you write even if you use pure SQL (and no Java objects)
* Persist and retrieve simple annotated Java objects, and lists thereof, _without you writing SQL_
* Persist and retrieve complex annotated Java objects, and lists thereof, _where you provide the SQL_

SansOrm will _never_...

* Perform a JOIN for you
* Persist a graph of objects for you
* Lazily retrieve anything for you
* Page data for you

These things that SansOrm will _never_ do are better and more efficiently performed by _you_.  SansOrm will _help_ you
do them simply, but there isn't much magic under the covers.

Note: SansOrm requires a JTA (transaction manager).  We recommend [Bitronix](http://docs.codehaus.org/display/BTM/Home), but other JTA like Atomikos will work as well.


### SqlClosure

We'll work from simple to complex.  In the first examples, the savings in code will not seem that great, but as we go
through the examples you'll notice the code using SansOrm vs. pure Java/JDBC gets gets more and more compact.

SansOrm provides you with two important classes.  Let's look at the first, which has nothing to do with Java objects or 
persistence.  This class just makes your life easier when writing raw SQL (JDBC).  It is called ```SqlClosure```.

Typical Java pure JDBC with [mostly] correct resource cleanup:
```Java
public int getUserCount(String usernameWildcard) throws SQLException {
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

Now the same code using SansOrm's ```SqlClosure``` (with _completely_ correct resource cleanup):
```Java
public int getUserCount(final String usernameWildcard) {
   return new SqlClosure<Integer>() {
      public Integer execute(Connection conn) {
          PreparedStatement stmt = autoClose(conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username LIKE ?"));
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
* The SqlClosure class will call your ```execute(Connection)``` method with a provided connection
   * The provided connection will be closed quietly automatically (i.e. exceptions in ```connection.close()``` will be eaten)
* The SqlClosure class offers an ```autoClose()``` method for Statements and ResultSets
   * The resource passed to ```autoClose()``` will be closed quietly automatically
* SqlExceptions thrown from the body of the ```execute()``` method will be wrapped in a RuntimeException

As mentioned above, the ```SqlClosure``` class is generic, and the signature looks something like this:
```Java
public class T SqlClosure<T> {
   public abstract T execute(Connection);
   public T execute() { ... }
}
```
```SqlClosure``` is typically constructed as an anonymous class, and you must provide the implementation of 
the ```execute(Connection connection)``` method.  Invoking the ```execute()``` method (no parameters) will create a
Connection and invoke your overridden method, cleaning up resources in a finally, and returning the value
returned by the overridden method.  Of course you don't have to execute the closure right away; you could stick it into 
a queue for later execution, pass it to another method, etc.  But typically you'll run execute it right away.

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
Even if you use no other features of SansOrm, the ```SqlClosure``` class alone can really help to cleanup and simplify
your code.

### Object Mapping
While the ```SqlClosure``` is extremly useful and helps reduce the boilerplate code that you write, we know why you're 
here: _object mapping_.  Let's jump right in with some examples.

Take this database table:
```SQL
CREATE TABLE customer (
   customer_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
   last_name VARCHAR(255),
   first_name VARCHAR(255),
   email VARCHAR(255)
);
```
Let's imagine a Java class that reflects the table in a straight-forward way, and contains some JPA (javax.persistence) annotations:

Customer:
```Java
@Table(name = "customer")
public class Customer {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "customer_id")
   private int customer_id;

   @Column(name = "last_name")
   private String lastName;

   @Column(name = "first_name")
   private String firstName;

   @Column(name = "email")
   private String emailAddress;

   public Customer() {
      // no arg constuctor declaration is necessary only when other constructors are declared
   }
}
```
Here we introduce another SansOrm class, ```OrmElf```.  What is ```OrmElf```?  Well, an 'Elf' is a 'Helper'
but with fewer letters to type.  Besides, who doesn't like Elves?  Let's look at how the ```OrmElf``` can help us:
```Java
public List<Customer> getAllCustomers() {
   return new SqlClosure<List<Customers>() {
      public List<Customer> execute(Connection connection) {
         PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM customer");
         return OrmElf.statementToList(pstmt, Customer.class);
      }
   }.execute();
}
```
The OrmElf will execute the ```PreparedStatement``` and using the annotations in the ```Customer``` class will
construct a ```List``` of ```Customer``` instances whose values come from the ```ResultSet```.  *Note that
```OrmElf``` will set the properties directly on the object, it does not use getter/setters.  Note also that
```autoClose()``` was not necessary, the OrmElf will close the statement automatically.*

Of course, in addition to querying, the ```OrmElf``` can perform basic operations such these (where ```customer```
is a ```Customer```):
* ```OrmElf.insertObject(connection, customer)```
* ```OrmElf.updateObject(connection, customer)```
* ```OrmElf.deleteObject(connection, customer)```

Let's make another example, somewhat silly, but showing how queries can be parameterized:
```Java
public List<Customer> getCustomersSillyQuery(final int minId, final int maxId, final String like) {
   return new SqlClosure<List<Customers>() {
      public List<Customer> execute(Connection connection) {
         PreparedStatement pstmt = connection.prepareStatement(
             "SELECT * FROM customer WHERE (customer_id BETWEEN ? AND ?) AND last_name LIKE ?"));
         return OrmElf.statementToList(pstmt, Customer.class, minId, maxId, like+"%");
      }
   }.execute();
}
```
Well, that's fairly handy. Note the use of varargs. Following the class parameter, zero or more parameters can be passed,
and will be used to set query parameters (in order) on the ```PreparedStatement```.

Materializing object instances from rows is so common, there are some further things the 'Elf' can help with.  Let's do 
the same thing as above, but using another helper method.
```Java
public List<Customer> getCustomersSillyQuery(final int minId, final int maxId, final String like) {
   return new SqlClosure<List<Customers>() {
      public List<Customer> execute(Connection connection) {
          return OrmElf.listFromClause(connection, Customer.class, "(customer_id BETWEEN ? AND ?) AND last_name LIKE ?",
                                       minId, maxId, like+"%");
      }
   }.execute();
}
```
Now we're cooking with gas!  The ```OrmElf``` will use the ```Connection``` that is passed, along with the annotations
on the ```Customer``` class to determine which table and columns to SELECT, and use the passed `clause` as the
WHERE portion of the statement (passing 'WHERE' explicitly is also supported), and finally it will use the passed 
parameters to set the query parameters.

While the ```SqlClosure``` is great, and you'll come to wonder how you did without it, for some simple cases like the
previous example, it adds a little bit of artiface around what could be even simpler.

Enter ```SqlClosureElf```.  Yes, another elf.
```Java
public List<Customer> getCustomersSillyQuery(int minId, int maxId, String like) {
   return SqlClosureElf.listFromClause(Customer.class, "(customer_id BETWEEN ? AND ?) AND last_name LIKE ?",
                                       minId, maxId, "%"+like+"%");
}
```
Here the ```SqlClosureElf``` is creating the ```SqlClosure``` under the covers as well as using the ```OrmElf``` to retrieve
the list of ```Customer``` instances. Like the ```OrmElf``` the ```SqlClosureElf``` exposes lots of methods for
common scenarios, a few are:
* ```SqlClosureElf.insertObject(customer)```
* ```SqlClosureElf.updateObject(customer)```
* ```SqlClosureElf.deleteObject(customer)```

### More Advanced

Just page as provided just a taste, so go on over to the [Advanced Usage](https://github.com/brettwooldridge/SansOrm/blob/master/doc/AdvancedUsage.md) page to go deep.
