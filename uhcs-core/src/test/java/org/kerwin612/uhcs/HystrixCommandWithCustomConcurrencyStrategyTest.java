package org.kerwin612.uhcs;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class HystrixCommandWithCustomConcurrencyStrategyTest {

  public static LinkedHashSet<Integer> result;

  @Before
  public void init() {
    result = new LinkedHashSet<>();
    HystrixPlugins.getInstance()
        .registerConcurrencyStrategy(
            new HystrixConcurrencyStrategy() {
              public <T> Callable<T> wrapCallable(Callable<T> callable) {
                result.add(1);
                return callable;
              }
            });
  }

  @Test
  public void testUniformHystrixConcurrencyStrategy() {
    UniformHystrixConcurrencyStrategy.register(
        new ArrayList<HystrixCallableWrapper>() {
          {
            add(
                new HystrixCallableWrapper() {
                  @Override
                  public <T> Callable<T> wrap(Callable<T> callable) {
                    result.add(2);
                    return callable;
                  }

                  @Override
                  public Integer getOrder() {
                    return 1;
                  }
                });
            add(
                new HystrixCallableWrapper() {
                  @Override
                  public <T> Callable<T> wrap(Callable<T> callable) {
                    result.add(3);
                    return callable;
                  }

                  @Override
                  public Integer getOrder() {
                    return 2;
                  }
                });
          }
        });

    new HystrixCommand<Boolean>(
        HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TEST"))) {
      @Override
      protected Boolean run() throws Exception {
        return true;
      }
    }.execute();

    assertEquals("231", result.stream().map(String::valueOf).collect(Collectors.joining("")));
  }
}
