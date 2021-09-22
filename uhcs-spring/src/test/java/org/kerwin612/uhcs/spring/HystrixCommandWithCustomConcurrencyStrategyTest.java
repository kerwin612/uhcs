package org.kerwin612.uhcs.spring;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kerwin612.uhcs.HystrixCallableWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HystrixCommandWithCustomConcurrencyStrategyTest {

  public static LinkedHashSet<Integer> result;

  @Before
  public void init() {
    result = new LinkedHashSet<>();
  }

  @Test
  public void testUniformHystrixConcurrencyStrategy() {
    new HystrixCommand<Boolean>(
        HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TEST"))) {
      @Override
      protected Boolean run() throws Exception {
        return true;
      }
    }.execute();

    assertEquals("231", result.stream().map(String::valueOf).collect(Collectors.joining("")));
  }

  @EnableUHCS
  @Configuration
  public static class Config4Test {

    @Bean
    HystrixConcurrencyStrategy strategy() {
      return new HystrixConcurrencyStrategy() {
        public <T> Callable<T> wrapCallable(Callable<T> callable) {
          result.add(1);
          return callable;
        }
      };
    }

    @Bean
    HystrixCallableWrapper wrapper1() {
      return new HystrixCallableWrapper() {
        @Override
        public <T> Callable<T> wrap(Callable<T> callable) {
          result.add(2);
          return callable;
        }

        @Override
        public Integer getOrder() {
          return 1;
        }
      };
    }

    @Bean
    HystrixCallableWrapper wrapper2() {
      return new HystrixCallableWrapper() {
        @Override
        public <T> Callable<T> wrap(Callable<T> callable) {
          result.add(3);
          return callable;
        }

        @Override
        public Integer getOrder() {
          return 2;
        }
      };
    }
  }
}
