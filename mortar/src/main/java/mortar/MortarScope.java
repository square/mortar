/*
 * Copyright 2013 Square Inc.
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
package mortar;

import android.content.Context;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;

public interface MortarScope {
  final String ROOT_NAME = "Root";
  final String NO_SEED = "No Seed";
  final String DIVIDER = ":";

  /**
   * Returns the name of this scope, used to retrieve it from its parent via {@link
   * #findChild(String)}.
   */
  String getName();

  String getPath();

  /**
   * Returns the seed for this scope.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  Object getSeed();

  Object getService(String serviceName);

  Object getServiceProvider(String serviceName);

  /**
   * Register the given {@link Scoped} instance to have its {@link Scoped#onEnterScope(MortarScope)}
   * and {@link Scoped#onExitScope()} methods called. Redundant registrations are safe,
   * they will not lead to additional calls to these two methods.
   * <p>
   * Calls to {@link Scoped#onEnterScope(MortarScope) onEnterScope} are dispatched asynchronously
   * if a previous {@code register} call is already in progress.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  void register(Scoped scoped);

  /**
   * Returns the child scope whose name matches the given, or null if there is none.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  MortarScope findChild(String name);

  Builder buildChild(String name);

  /**
   * Creates a new Context based on the given parent and this scope.
   */
  Context createContext(Context parentContext);

  /**
   * Sends {@link Scoped#onExitScope()} to all registrants and then clears the
   * registration list. Recursively destroys all children. Parent scope drops its reference
   * to this instance. Redundant calls to this method are safe.
   */
  void destroy();

  /** Returns true if this scope has been destroyed, false otherwise. */
  boolean isDestroyed();

  class Builder {
    private final String name;
    private final RealScope parent;
    private final Map<String, MortarServiceProvider> serviceProviders = new LinkedHashMap<>();

    private Object seed = NO_SEED;

    public static Builder ofRoot() {
      return new Builder(ROOT_NAME, null);
    }

    Builder(String name, RealScope parent) {
      this.name = name;
      this.parent = parent;
    }

    public Builder withSeed(Object seed) {
      if (this.seed != NO_SEED) {
        throw new IllegalArgumentException(
            format("New scope \"%s\" seed already set to %s, cannot replace with %s", name,
                this.seed, seed));
      }
      this.seed = seed;
      return this;
    }

    public Builder withService(MortarServiceProvider provider) {
      String serviceName = provider.getName();
      MortarServiceProvider existing = serviceProviders.put(serviceName, provider);
      if (existing != null) {
        throw new IllegalArgumentException(
            format("New scope \"%s\" already bound to service %s, cannot be rebound to %s", name,
                existing, provider));
      }
      return this;
    }

    public MortarScope build() {
      RealScope newScope = new RealScope(name, parent, seed, serviceProviders);
      if (parent != null) {
        parent.children.put(name, newScope);
      }
      return newScope;
    }
  }
}
