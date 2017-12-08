/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehcache.clustered.client.config.builders;

import java.net.URI;

import org.ehcache.clustered.client.config.ClusteringServiceConfiguration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.ehcache.clustered.client.config.Timeouts;
import org.ehcache.clustered.common.ServerSideConfiguration;
import org.ehcache.config.Builder;

/**
 * A builder of ClusteringService configurations.
 */
public final class ClusteringServiceConfigurationBuilder implements Builder<ClusteringServiceConfiguration> {

  private final URI clusterUri;
  private final Timeouts timeouts;
  private final boolean autoCreate;

  /**
   * Creates a new builder connecting to the given cluster.
   *
   * @param clusterUri cluster URI
   *
   * @return a clustering service configuration builder
   */
  public static ClusteringServiceConfigurationBuilder cluster(URI clusterUri) {
    return new ClusteringServiceConfigurationBuilder(clusterUri, Timeouts.builder().build(), false);
  }

  private ClusteringServiceConfigurationBuilder(URI clusterUri, Timeouts timeouts, boolean autoCreate) {
    this.clusterUri = Objects.requireNonNull(clusterUri, "Cluster URI can't be null");
    this.timeouts = Objects.requireNonNull(timeouts, "Timeouts can't be null");
    this.autoCreate = autoCreate;
  }

  /**
   * Support connection to an existing entity or create if the entity if absent.
   *
   * @return a clustering service configuration builder
   */
  public ServerSideConfigurationBuilder autoCreate() {
    return new ServerSideConfigurationBuilder(new ClusteringServiceConfigurationBuilder(this.clusterUri, this.timeouts, true));
  }

  /**
   * Only support connection to an existing entity.
   *
   * @return a clustering service configuration builder
   */
  public ServerSideConfigurationBuilder expecting() {
    return new ServerSideConfigurationBuilder(new ClusteringServiceConfigurationBuilder(this.clusterUri, this.timeouts, false));
  }

  /**
   * Adds timeouts.
   * Read operations which time out return a result comparable to a cache miss.
   * Write operations which time out won't do anything.
   * Lifecycle operations which time out will fail with exception
   *
   * @param timeouts the amount of time permitted for all operations
   *
   * @return a clustering service configuration builder
   *
   * @throws NullPointerException if {@code timeouts} is {@code null}
   */
  public ClusteringServiceConfigurationBuilder timeouts(Timeouts timeouts) {
    return new ClusteringServiceConfigurationBuilder(this.clusterUri, timeouts, this.autoCreate);
  }

  /**
   * Adds timeouts.
   * Read operations which time out return a result comparable to a cache miss.
   * Write operations which time out won't do anything.
   * Lifecycle operations which time out will fail with exception
   *
   * @param timeoutsBuilder the builder for amount of time permitted for all operations
   *
   * @return a clustering service configuration builder
   *
   * @throws NullPointerException if {@code timeouts} is {@code null}
   */
  public ClusteringServiceConfigurationBuilder timeouts(Timeouts.Builder timeoutsBuilder) {
    return new ClusteringServiceConfigurationBuilder(this.clusterUri, timeoutsBuilder.build(), this.autoCreate);
  }

  /**
   * Adds a read operation timeout.  Read operations which time out return a result comparable to
   * a cache miss.
   *
   * @param duration the amount of time permitted for read operations
   * @param unit the time units for {@code duration}
   *
   * @return a clustering service configuration builder
   *
   * @throws NullPointerException if {@code unit} is {@code null}
   * @throws IllegalArgumentException if {@code amount} is negative
   *
   * @deprecated
   */
  @Deprecated
  public ClusteringServiceConfigurationBuilder readOperationTimeout(long duration, TimeUnit unit) {
    Duration readTimeout = Duration.of(duration, toChronoUnit(unit));
    return timeouts(Timeouts.builder().setReadOperationTimeout(readTimeout).build());
  }

  @Override
  public ClusteringServiceConfiguration build() {
    return new ClusteringServiceConfiguration(clusterUri, timeouts, autoCreate, null);
  }

  /**
   * Internal method to build a new {@link ClusteringServiceConfiguration} from the {@link ServerSideConfigurationBuilder}.
   *
   * @param serverSideConfiguration the {@code ServerSideConfiguration} to use
   *
   * @return a new {@code ClusteringServiceConfiguration} instance built from {@code this}
   *        {@code ClusteringServiceConfigurationBuilder} and the {@code serverSideConfiguration} provided
   */
  ClusteringServiceConfiguration build(ServerSideConfiguration serverSideConfiguration) {
    return new ClusteringServiceConfiguration(clusterUri, timeouts, autoCreate, serverSideConfiguration);
  }

  private static ChronoUnit toChronoUnit(TimeUnit unit) {
    if(unit == null) {
      return null;
    }
    switch (unit) {
      case NANOSECONDS:  return ChronoUnit.NANOS;
      case MICROSECONDS: return ChronoUnit.MICROS;
      case MILLISECONDS: return ChronoUnit.MILLIS;
      case SECONDS:      return ChronoUnit.SECONDS;
      case MINUTES:      return ChronoUnit.MINUTES;
      case HOURS:        return ChronoUnit.HOURS;
      case DAYS:         return ChronoUnit.DAYS;
      default: throw new AssertionError("Unknown unit: " + unit);
    }
  }

}
