# SansORM

[![][Build Status img]][Build Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

## Preface

Even if you do "pure JDBC", you will find SansOrm's utility classes extremely useful.  SansOrm is a "No-ORM" sane
Java-to-SQL/SQL-to-Java object mapping library.  It was created to rid my company's product of Hibernate.  After about
10 years of using ORMs  in various projects, I came to the same conclusion as others: 
[ORM is an Anti-Pattern](https://github.com/brettwooldridge/SansOrm/wiki/ORM-is-an-anti-pattern).

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

You could consider the philosophy of SansOrm to be SQL-first.  That is, think about a correct SQL relational schema *first*, and then once that is correct, consider how to use SansOrm to make your life easier.  In order to scale, your SQL schema design and the queries that run against it need to be efficient.  There is no way to go from an "object model" to SQL with any kind of real efficiency, due to an inherent mis-match between the "object world" and the "relational world".  As others have noted, if you truly need to develop in the currency of pure objects, then what you need is not a relational database but instead an object database.

**Note:** *SansOrm does not currently support MySQL because the MySQL JDBC driver does not return proper metadata
which is required by SansOrm for mapping.  In the future, SansOrm may support a purely 100% annotation-based type
mapping but this would merely be a concession to MySQL and in no way desirable.*

----------------------------------------------------------------

<img src="https://github.com/brettwooldridge/SansOrm/wiki/quote1.png"/>

----------------------------------------------------------------

### Initialization

First of all we need a datasource. Once you get it, call one of ```SansOrm.initializeXXX``` methods:
```Java
DataSource ds = ...;
SansOrm.initializeTxNone(ds);

// or if you want to use embedded TransactionManager implementation
SansOrm.initializeTxSimple(ds);

// or if you have your own TransactionManager and UserTransaction
TransactionManager tm = ...;
UserTransaction ut = ...;
SansOrm.initializeTxCustom(ds, tm, ut);
```
We strongly recommend using the embedded ``TransactionManager`` via the the second initializer above.  If you have an existing external ``TransactionManager``, of course you can use that.

The embedded ``TransactionManager`` conserves database Connections when nested methods are called, alleviating the need to pass ``Connection`` instances around manually.  For example:
```Java
List<User> getUsers(String lastNamePrefix) {
   return SqlClosure.sqlExecute( connection -> {       // <-- Transaction started, Connection #1 acquired.
      final List<Users> users =
         OrmElf.listFromClause(connection, User.class, "last_name LIKE ?", lastNamePrefix + "%");

      return populateRoles(users);
   }
   // Transaction automatically committed at the end of the execute() call.
}

List<User> populatePermissions(final List<User> users) {
   return SqlClosure.sqlExecute( connection -> {       // <-- Transaction in-progress, Connection #1 re-used.
      for (User user : users) {
         user.setPermissions(OrmElf.listFromClause(connection, Permission.class, "user_id=?", user.getId());
      }
      return users;
   }
   // Transaction will be committed at the end of the execute() call in getUsers() above.
}
```
The ``TransactionManager`` uses a ``ThreadLocal`` variable to "flow" the transaction across nested calls, allowing all work to be committed as a single unit of work.  Additionally, ``Connection`` resources are conserved.  Without a ``TransactionManager``, the above code would require two ``Connections`` to be borrowed from a pool.

### SqlClosure

We'll work from simple to complex.  In the first examples, the savings in code will not seem that great, but as we go
through the examples you'll notice the code using SansOrm vs. pure Java/JDBC gets more and more compact.

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
          PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username LIKE ?");
          stmt.setString(1, usernameWildcard);
          ResultSet resultSet = stmt.executeQuery();
          return (resultSet.next() ? resultSet.getInt(1) : 0;
      }
   }.execute();
}
```
Important points:
* The SqlClosure class is a generic (templated) class
* The SqlClosure class will call your ```execute(Connection)``` method with a provided connection
   * The provided connection will be closed quietly automatically (i.e. exceptions in ```connection.close()``` will be eaten)
* SqlExceptions thrown from the body of the ```execute()``` method will be wrapped in a RuntimeException

**Now with a Java 8 Lambda** <br>
```java
public int getUserCount(final String usernameWildcard) {
   return SqlClosure.sqlExecute(connection -> {
      PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username LIKE ?"));
      stmt.setString(1, usernameWildcard);
      ResultSet resultSet = stmt.executeQuery();
      return (resultSet.next() ? resultSet.getInt(1) : 0;
   });
}
```
Note that the lambda automatically closes Statement and ResultSet resources.

As mentioned above, the ```SqlClosure``` class is generic, and the signature looks something like this:
```Java
public class T SqlClosure<T> {
   public abstract T execute(Connection);
   public T execute();
   public static <V> V sqlExecute(final SqlVarArgsFunction<V> functional, final Object... args);
}
```
```SqlClosure``` is typically constructed as an anonymous class, and you must provide the implementation of 
the ```execute(Connection connection)``` method.  Invoking the ```execute()``` method (no parameters) will create a
Connection and invoke your overridden method, cleaning up resources in a finally, and returning the value
returned by the overridden method.  Of course you don't have to execute the closure right away; you could stick it into 
a queue for later execution, pass it to another method, etc.  But typically you'll run execute it right away.

More common still is using **Java 8 Lambdas**.

Let's look at an example of returning a complex type:
```Java
public Set<String> getAllUsernames() {
   return new SqlClosure<Set<String>>() {
      public Set<String> execute(Connection connection) {
         Set<String> usernames = new HashSet<>();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT username FROM users");
         while (resultSet.next()) {
            usernames.add(resultSet.getString("username"));
         }
         return usernames;
      }
   }.execute();
}
```
**And again with Java 8 Lambda** <br>
```Java
public Set<String> getAllUsernames() {
   return SqlClosure.sqlExecute(connection -> {
      Set<String> usernames = new HashSet<>();
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT username FROM users");
      while (resultSet.next()) {
         usernames.add(resultSet.getString("username"));
      }
      return usernames;
   });
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
   return SqlClosure.sqlExecute( connection -> {
      PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM customer");
      return OrmElf.statementToList(pstmt, Customer.class);
   });
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
   return SqlClosure.sqlExecute( conn -> {
      PreparedStatement pstmt = conn.prepareStatement(
         "SELECT * FROM customer WHERE (customer_id BETWEEN ? AND ?) AND last_name LIKE ?"));
      return OrmElf.statementToList(pstmt, Customer.class, minId, maxId, like+"%");
   });
}
```
Well, that's fairly handy. Note the use of varargs. Following the class parameter, zero or more parameters can be passed,
and will be used to set query parameters (in order) on the ```PreparedStatement```.

Materializing object instances from rows is so common, there are some further things the 'Elf' can help with.  Let's do 
the same thing as above, but using another helper method.
```Java
public List<Customer> getCustomersSillyQuery(final int minId, final int maxId, final String like) {
   return SqlClosure.sqlExecute( connection -> {
      return OrmElf.listFromClause(connection, Customer.class,
                                   "(customer_id BETWEEN ? AND ?) AND last_name LIKE ?",
                                   minId, maxId, like+"%");
   });
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
   return SqlClosureElf.listFromClause(Customer.class, 
                                       "(customer_id BETWEEN ? AND ?) AND last_name LIKE ?",
                                       minId, maxId, "%"+like+"%");
}
```
Here the ```SqlClosureElf``` is creating the ```SqlClosure``` under the covers as well as using the ```OrmElf``` to retrieve
the list of ```Customer``` instances. Like the ```OrmElf``` the ```SqlClosureElf``` exposes lots of methods for
common scenarios, a few are:
* ```SqlClosureElf.insertObject(customer)```
* ```SqlClosureElf.updateObject(customer)```
* ```SqlClosureElf.deleteObject(customer)```

### Supported Annotations
Except for the ``@Table`` and ``@MappedSuperclass`` annotations, which must annotate a *class*, all other annotations must appear on *member variables*.  Annotations on *getter/setter* methods are not supported.  SansOrm will get/set member variables directly through reflection during read/write operations.

The following annotations are supported:

| Annotation            | Supported Attributes                                 |
|:--------------------- |:---------------------------------------------------- |
| ``@Column``           | ``name``, ``insertable``, ``updatable``, ``table``   |
| ``@Convert``          | ``converter`` (``AttributeConverter`` _classes only_)|
| ``@Enumerated``       | ``value`` (=``EnumType.ORDINAL``, ``EnumType.STRING``) |
| ``@GeneratedValue``   | ``strategy`` (``GenerationType.IDENTITY`` _only_)    |
| ``@Id``               | n/a                                                  |
| ``@JoinColumn``       | ``name`` (supports **self-join** _only_)             |
| ``@MappedSuperclass`` | n/a                                                  |
| ``@Table``            | ``name``                                             |
| ``@Transient``        | n/a                                                  |

By default, SansOrm will *lower-case* all ``name`` and ``table`` attribute values, which is fine for DML case-_insensitive_ databases such as PostgreSQL, Derby, Oracle, etc.  However, some databases are case-sensitive with respect to identifiers, such as H2. Therefore, SansOrm supports *case-sensitive* databases through the use of quoted identifiers.

Quoted identifer example:
```java
@Table(name = "\"Customer\"")
class Customer {
   @Column(name = "\"Last_Name\"")
   String lastName;
   ...
}
```

### Automatic Data Type Conversions

#### Writing
When *writing* data to JDBC, SansOrm relies on the *driver* to perform most conversions.  SansOrm only calls ``Statement.setObject()`` internally, and expects that the driver will properly perform conversions.  For example, convert an ``int`` or ``java.lang.Integer`` into an ``INTEGER`` column type.

If the ``@Convert`` annotation is present on the field in question, the appropriate user-specified ``javax.persistence.AttributeConverter`` will be called. 

For fields where the ``@Enumerated`` annotation is present, SansOrm will obtain the value to persist by calling ``ordinal()`` on the ``enum`` instance in the case of ``EnumType.ORDINAL``, and ``name()`` on the ``enum`` instance in the case of ``EnumType.STRING``.

#### Reading
When *reading* data from JDBC, SansOrm relies on the *driver* to perform most conversions.  SansOrm only calls ``ResultSet.getObject()`` internally, and expects that the driver will properly perform conversions to Java types.  For example , for an ``INTEGER`` column type, return a ``java.lang.Integer`` from ``ResultSet.getObject()``.

However, if the Java object type returned by the driver *does not match* the type of the mapped member field, SansOrm permits the following automatic conversions:

| Driver ``getObject()`` Java Type | Mapped Member Java type                 |
|:-------------------------------- |:--------------------------------------- |
| ``java.lang.Integer``            | ``boolean`` (0 == ``false``, everything else ``true``)|
| ``java.math.BigDecimal``         | ``java.math.BigInteger``  |
| ``java.math.BigDecimal``         | ``int`` or ``java.lang.Integer`` (via cast)  |
| ``java.math.BigDecimal``         | ``long`` or ``java.lang.Long`` (via cast) |
| ``java.math.BigDecimal``         | ``double`` or ``java.lang.Double`` (via cast) |
| ``java.util.UUID``               | ``String``                                |
| ``java.sql.Clob``                | ``String``                                |

If the ``@Convert`` annotation is present on the field in question, the appropriate user-specified ``javax.persistence.AttributeConverter`` will be called. 

For fields where the ``@Enumerated`` annotation is present, SansOrm will map ``java.lang.Integer`` values from the driver to the correct ``Enum`` value in the case of ``EnumType.ORDINAL``, and will map ``java.lang.String`` values from the driver to the correct ``Enum`` value in the case of ``EnumType.STRING``.

Finally, SansOrm has specific support for the PostgreSQL ``PGobject`` and ``CITEXT`` data types.  ``CITEXT`` column values are converted to ``java.lang.String``.  ``PGobject`` "unknown type" column values have their ``getValue()`` method called, and the result is attempted to be set via reflection onto the mapped member field.

### More Advanced

Just page as provided just a taste, so go on over to the [Advanced Usage](https://github.com/brettwooldridge/SansOrm/blob/master/doc/AdvancedUsage.md) page to go deep.


[Build Status]:https://travis-ci.org/brettwooldridge/SansOrm
[Build Status img]:https://travis-ci.org/brettwooldridge/SansOrm.svg?branch=master

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
   
[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/sansorm
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/sansorm/badge.svg
   
[Javadocs]:http://javadoc.io/doc/com.zaxxer/sansorm
[Javadocs img]:http://javadoc.io/badge/com.zaxxer/sansorm.svg
