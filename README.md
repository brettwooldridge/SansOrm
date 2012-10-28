## Preface

SansOrm is a "No-ORM" sane Java-to-SQL/SQL-to-Java object mapping library.  It was created to rid my company's product of Hibernate.  After about 10 years of using ORMs in various projects, I came to the same conclusion as others: [ORM is an Anti-Pattern](https://github.com/brettwooldridge/SansOrm/wiki/ORM-is-an-anti-pattern).

TL;DR:

*  No ORM will ever scale.
*  Don't fear the SQL.

Either you buy into it, or you don't.  If you don't, well, buh-bye.

## SansOrm

SansOrm is not an ORM.  SansOrm library will...

*  Massively decrease the boilerplate code you write even if you use pure SQL (and no Java objects)
*  Persist and retrieve simple annotated Java objects, and lists thereof, _without you writing SQL_
*  Persist and retrieve simple annotated Java objects, and lists thereof, _where you provide the SQL_

SansOrm will _never_...

*  Perform a JOIN for you
*  Persist a nested hierarchy of objects for you
*  Lazily retrieve anything for you
*  Page data for you

These things that SansOrm will _never_ do are better and more efficiently performed by _you_.  SansOrm will _help_ you do them simply, but there is no magic under the covers.

### Two Important Classes

We'll work from simle to complex.  In the first examples, the savings in code will not seem that great, but as we go through the examples you'll notice the code using SansOrm vs. pure Java/JDBC gets gets more and more compact.

SansOrm provides you with two important classes.  Let's look at the first, which has nothing to do with Java objects or persistence.  This class just makes your life easier when writing raw SQL (JDBC).  It is called _SqlClosure_.

Typical Java pure JDBC with correct resource cleanup:

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


SansOrm:

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

