# UHCS
**Uniform Hystrix Concurrency Strategy**

### Use in non-spring projects:

Add dependency: 
```xml
<dependency>
    <groupId>io.github.kerwin612</groupId>
    <artifactId>uhcs-core</artifactId>
    <version>0.0.3</version>
</dependency>
```
Register Strategy:
```java
UniformHystrixConcurrencyStrategy.register(
  new ArrayList<HystrixCallableWrapper>() {
    {
      add(
          new HystrixCallableWrapper() {
            @Override
            public <T> Callable<T> wrap(Callable<T> callable) {
              //set context
              return () -> {
                //get context
                return callable.call();
              };
            }
          }
      );
    }
});
```


### Use in spring projects:

Add dependency: 
```xml
<dependency>
    <groupId>io.github.kerwin612</groupId>
    <artifactId>uhcs-spring</artifactId>
    <version>0.0.3</version>
</dependency>
```
Register Strategy:
```java
@Component
class HystrixCallableWrapperImpl implements HystrixCallableWrapper {
  @Override
  public <T> Callable<T> wrap(Callable<T> callable) {
    //set context
    return () -> {
      //get context
      return callable.call();
    };
  }
}
```
