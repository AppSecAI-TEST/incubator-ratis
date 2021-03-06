/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ratis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * General Java utility methods.
 */
public interface JavaUtils {
  Logger LOG = LoggerFactory.getLogger(JavaUtils.class);

  /**
   * Invoke {@link Callable#call()} and, if there any,
   * wrap the checked exception by {@link RuntimeException}.
   */
  static <T> T callAsUnchecked(Callable<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the value from the future and then consume it.
   */
  static <T> void getAndConsume(CompletableFuture<T> future, Consumer<T> consumer) {
    final T t;
    try {
      t = future.get();
    } catch (Exception ignored) {
      LOG.warn("Failed to get()", ignored);
      return;
    }
    consumer.accept(t);
  }

  /**
   * Create a memoized supplier which gets a value by invoking the initializer once
   * and then keeps returning the same value as its supplied results.
   *
   * @param initializer to supply at most one non-null value.
   * @param <T> The supplier result type.
   * @return a memoized supplier which is thread-safe.
   */
  static <T> Supplier<T> memoize(Supplier<T> initializer) {
    Objects.requireNonNull(initializer, "initializer == null");
    return new Supplier<T>() {
      private volatile T value = null;

      @Override
      public T get() {
        T v = value;
        if (v == null) {
          synchronized (this) {
            v = value;
            if (v == null) {
              v = value = Objects.requireNonNull(initializer.get(),
                  "initializer.get() returns null");
            }
          }
        }
        return v;
      }
    };
  }

  Supplier<ThreadGroup> ROOT_THREAD_GROUP = memoize(() -> {
    for (ThreadGroup g = Thread.currentThread().getThreadGroup(), p;; g = p) {
      if ((p = g.getParent()) == null) {
        return g;
      }
    }
  });

  static ThreadGroup getRootThreadGroup() {
    return ROOT_THREAD_GROUP.get();
  }
}
