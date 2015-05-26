# FreeMapper

FreeMapper is a Java Annotation Processor that automatically generates classes for mapping from SQL
`ResultSet`s to POJOs without reflection. Traditional ORMs add a ton of complexity and can have a
huge impact on performance. The goal of FreeMapper is to get most of the developer-friendly features
of ORMs without any of the hidden complexity or performance impact.

## Usage

FreeMapper expects your POJOs to have a `Builder` child class that functions just like a builder
class created by [FreeBuilder](https://github.com/google/FreeBuilder). For example, given the
following class:

```java
import org.inferred.freebuilder.FreeBuilder;
import org.joeyb.freemapper.Field;
import org.joeyb.freemapper.FreeMapper;

import java.util.Date;
import java.util.Optional;

@FreeBuilder
@FreeMapper
public interface User {

  @Field(name = "email")
  String getEmail();

  @Field(name = "name")
  Optional<String> getName();

  @Field(name = "age")
  Optional<Integer> getAge();

  @Field(name = "is_admin")
  boolean isAdmin();

  @Field(name = "created_at")
  Date getCreatedAt();

  class Builder extends User_Builder { }

  class Mapper extends User_Mapper { }
}
```

FreeMapper will build a `User_Mapper` class that looks like this:

```java
import java.lang.FunctionalInterface;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

abstract class User_Mapper {
  public static User.Builder mapBuilder(ResultSet rs) throws SQLException {
    User.Builder b = new User.Builder();
    b.setEmail(rs.getString("email"));
    b.setName(getOptionalValue(rs, () -> rs.getString("name")));
    b.setAge(getOptionalValue(rs, () -> rs.getInt("age")));
    b.setAdmin(rs.getBoolean("is_admin"));
    b.setCreatedAt(rs.getTimestamp("created_at"));
    return b;
  }

  public static User map(ResultSet rs) throws SQLException {
    return mapBuilder(rs).build();
  }

  public static List<User> mapAll(ResultSet rs) throws SQLException {
    List<User> list = new LinkedList<User>();
    while(rs.next()) {
      list.add(map(rs));
    }
    return list;
  }

  private static <T> Optional<T> getOptionalValue(ResultSet rs, ResultSetSupplier<T> getValue) throws SQLException {
    T value = getValue.get();
    return rs.wasNull() ? Optional.empty() : Optional.of(value);
  }

  @FunctionalInterface
  private interface ResultSetSupplier<T> {
    T get() throws SQLException;
  }
}
```