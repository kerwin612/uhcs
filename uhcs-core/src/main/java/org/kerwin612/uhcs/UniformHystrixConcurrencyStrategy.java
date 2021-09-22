package org.kerwin612.uhcs;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UniformHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

  private static HystrixConcurrencyStrategy currentStrategy() {
    return HystrixPlugins.getInstance().getConcurrencyStrategy();
  }

  public static HystrixConcurrencyStrategy register(
      HystrixConcurrencyStrategy hystrixConcurrencyStrategy) {
    if (hystrixConcurrencyStrategy == null) return currentStrategy();
    // Keeps reference of existing Hystrix plugins.
    HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
    HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
    HystrixPropertiesStrategy propertiesStrategy =
        HystrixPlugins.getInstance().getPropertiesStrategy();
    HystrixCommandExecutionHook commandExecutionHook =
        HystrixPlugins.getInstance().getCommandExecutionHook();

    HystrixPlugins.reset();
    HystrixPlugins.getInstance().registerConcurrencyStrategy(hystrixConcurrencyStrategy);

    // Registers existing plugins excepts the Concurrent Strategy plugin.
    HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
    HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
    HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
    HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);

    return hystrixConcurrencyStrategy;
  }

  public static HystrixConcurrencyStrategy register(
      Collection<HystrixCallableWrapper> hystrixCallableWrappers) {
    return register(null, hystrixCallableWrappers);
  }

  public static HystrixConcurrencyStrategy register(
      HystrixConcurrencyStrategy existingConcurrencyStrategy,
      Collection<HystrixCallableWrapper> hystrixCallableWrappers) {
    return new UniformHystrixConcurrencyStrategy(
            existingConcurrencyStrategy != null ? existingConcurrencyStrategy : currentStrategy(),
            hystrixCallableWrappers)
        .register();
  }

  private HystrixConcurrencyStrategy existingConcurrencyStrategy;

  private Collection<HystrixCallableWrapper> hystrixCallableWrappers;

  public UniformHystrixConcurrencyStrategy(
      Collection<HystrixCallableWrapper> hystrixCallableWrappers) {
    this(null, hystrixCallableWrappers);
  }

  public UniformHystrixConcurrencyStrategy(
      HystrixConcurrencyStrategy existingConcurrencyStrategy,
      Collection<HystrixCallableWrapper> hystrixCallableWrappers) {
    this.wrapStrategy(existingConcurrencyStrategy).callableWrappers(hystrixCallableWrappers);
  }

  public HystrixConcurrencyStrategy register() {
    return register(this);
  }

  public UniformHystrixConcurrencyStrategy wrapStrategy(
      HystrixConcurrencyStrategy existingConcurrencyStrategy) {
    this.existingConcurrencyStrategy = existingConcurrencyStrategy;
    return this;
  }

  public UniformHystrixConcurrencyStrategy callableWrappers(
      Collection<HystrixCallableWrapper> hystrixCallableWrappers) {
    this.hystrixCallableWrappers =
        hystrixCallableWrappers == null
            ? null
            : hystrixCallableWrappers.stream()
                .sorted(Comparator.comparingInt(HystrixCallableWrapper::getOrder))
                .collect(Collectors.toList());
    return this;
  }

  @Override
  public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
    return existingConcurrencyStrategy != null
        ? existingConcurrencyStrategy.getBlockingQueue(maxQueueSize)
        : super.getBlockingQueue(maxQueueSize);
  }

  @Override
  public <T> HystrixRequestVariable<T> getRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
    return existingConcurrencyStrategy != null
        ? existingConcurrencyStrategy.getRequestVariable(rv)
        : super.getRequestVariable(rv);
  }

  @Override
  public ThreadPoolExecutor getThreadPool(
      HystrixThreadPoolKey threadPoolKey,
      HystrixProperty<Integer> corePoolSize,
      HystrixProperty<Integer> maximumPoolSize,
      HystrixProperty<Integer> keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue) {
    return existingConcurrencyStrategy != null
        ? existingConcurrencyStrategy.getThreadPool(
            threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue)
        : super.getThreadPool(
            threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
  }

  @Override
  public ThreadPoolExecutor getThreadPool(
      HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolProperties threadPoolProperties) {
    return existingConcurrencyStrategy != null
        ? existingConcurrencyStrategy.getThreadPool(threadPoolKey, threadPoolProperties)
        : super.getThreadPool(threadPoolKey, threadPoolProperties);
  }

  @Override
  public <T> Callable<T> wrapCallable(Callable<T> callable) {
    Callable wrapCallable =
        new CallableWrapperChain(
                callable,
                hystrixCallableWrappers == null ? null : hystrixCallableWrappers.iterator())
            .wrapCallable();
    return existingConcurrencyStrategy != null
        ? existingConcurrencyStrategy.wrapCallable(wrapCallable)
        : super.wrapCallable(wrapCallable);
  }

  private static class CallableWrapperChain<T> {

    private final Callable<T> callable;

    private final Iterator<HystrixCallableWrapper> wrappers;

    CallableWrapperChain(Callable<T> callable, Iterator<HystrixCallableWrapper> wrappers) {
      this.callable = callable;
      this.wrappers = wrappers;
    }

    Callable<T> wrapCallable() {
      Callable<T> delegate = callable;
      while (wrappers != null && wrappers.hasNext()) {
        delegate = wrappers.next().wrap(delegate);
      }
      return delegate;
    }
  }
}
