package org.kerwin612.uhcs.spring;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.kerwin612.uhcs.HystrixCallableWrapper;
import org.kerwin612.uhcs.UniformHystrixConcurrencyStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EnableUHCS.UHCSAutoConfiguration.class)
public @interface EnableUHCS {

  @Configuration
  @ConditionalOnClass(HystrixConcurrencyStrategy.class)
  @ConditionalOnProperty(
      prefix = "uhcs",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  class UHCSAutoConfiguration {

    @Autowired UniformHystrixConcurrencyStrategy uniformHystrixConcurrencyStrategy;

    @Autowired(required = false)
    HystrixConcurrencyStrategy hystrixConcurrencyStrategy;

    @Bean
    @Primary
    UniformHystrixConcurrencyStrategy uniformHystrixConcurrencyStrategy(
        @Autowired(required = false) List<HystrixCallableWrapper> hystrixCallableWrappers) {
      return new UniformHystrixConcurrencyStrategy(
          hystrixConcurrencyStrategy, hystrixCallableWrappers);
    }

    @PostConstruct
    void register() {
      ((hystrixConcurrencyStrategy == null
                  || hystrixConcurrencyStrategy.equals(uniformHystrixConcurrencyStrategy))
              ? uniformHystrixConcurrencyStrategy
              : uniformHystrixConcurrencyStrategy.wrapStrategy(hystrixConcurrencyStrategy))
          .register();
    }
  }
}
