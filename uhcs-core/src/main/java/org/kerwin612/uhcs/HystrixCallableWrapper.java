package org.kerwin612.uhcs;

import java.util.concurrent.Callable;

public interface HystrixCallableWrapper {

  <T> Callable<T> wrap(Callable<T> callable);

  default Integer getOrder() {
    return 0;
  }
}
