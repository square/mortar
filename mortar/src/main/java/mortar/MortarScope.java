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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;

public class MortarScope {
  public static final String DIVIDER = ">>>";
  public static final String ROOT_NAME = "Root";
  public static final String SERVICE_NAME = MortarScope.class.getName();

  public static MortarScope getScope(Context context) {
    return (MortarScope) context.getSystemService(MortarScope.SERVICE_NAME);
  }

  public static MortarScope findChild(Context context, String name) {
    return getScope(context).findChild(name);
  }

  public static Builder buildChild(Context context, String name) {
    return getScope(context).buildChild(name);
  }

  public static Builder buildRootScope() {
    return new Builder(ROOT_NAME, null);
  }

  final Map<String, MortarScope> children = new LinkedHashMap<>();

  private boolean dead;

  private final Set<Scoped> tearDowns = new HashSet<>();
  final MortarScope parent;
  private final String name;
  private final Map<String, Object> services;

  MortarScope(String name, MortarScope parent, Map<String, Object> services) {
    this.parent = parent;
    this.name = name;
    this.services = services;
  }

  /**
   * Returns the name of this scope, used to retrieve it from its parent via {@link
   * #findChild(String)}.
   */
  public final String getName() {
    return name;
  }

  public String getPath() {
    if (parent == null) return getName();
    return parent.getPath() + DIVIDER + getName();
  }

  public boolean hasService(String serviceName) {
    return !isDestroyed() && findService(serviceName) != null;
  }

  /**
   * Returns the service associated with the given name.
   *
   * @throws IllegalArgumentException if no such service can be found
   * @see #hasService
   */
  public <T> T getService(String serviceName) {
    T service = findService(serviceName);
    if (service == null) {
      throw new IllegalArgumentException(format("No service found named \"%s\"", serviceName));
    }

    return service;
  }

  @SuppressWarnings("unchecked") //
  private <T> T findService(String serviceName) {
    if (!isDestroyed()) {
      if (MortarScope.class.getName().equals(serviceName)) return (T) this;

      T service = (T) services.get(serviceName);
      if (service != null) return service;

      if (parent != null) return parent.findService(serviceName);
    }

    return null;
  }

  /**
   * Register the given {@link Scoped} instance to have its {@link Scoped#onEnterScope(MortarScope)}
   * and {@link Scoped#onExitScope()} methods called. Redundant registrations are safe,
   * they will not lead to additional calls to these two methods.
   * <p>
   * Calls to {@link Scoped#onEnterScope(MortarScope) onEnterScope} are dispatched asynchronously.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  public void register(Scoped scoped) {
    assertNotDead();
    if (tearDowns.add(scoped)) scoped.onEnterScope(this);
  }

  /**
   * Returns the child scope whose name matches the given, or null if there is none.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  public MortarScope findChild(String childName) {
    assertNotDead();
    return children.get(childName);
  }

  public Builder buildChild(String childName) {
    assertNotDead();

    if (childName.contains(DIVIDER)) {
      throw new IllegalArgumentException(
          format("Name \"%s\" must not contain '%s'", childName, DIVIDER));
    }

    if (children.containsKey(childName)) {
      throw new IllegalArgumentException(
          format("Scope \"%s\" already has a child named \"%s\"", name, childName));
    }

    return new Builder(childName, this);
  }

  /**
   * Creates a new Context based on the given parent and this scope.
   */
  public Context createContext(Context parentContext) {
    return new MortarContextWrapper(parentContext, this);
  }

  /** Returns true if this scope has been destroyed, false otherwise. */
  public boolean isDestroyed() {
    return dead;
  }

  /**
   * Sends {@link Scoped#onExitScope()} to all registrants and then clears the
   * registration list. Recursively destroys all children. Parent scope drops its reference
   * to this instance. Redundant calls to this method are safe.
   */
  public void destroy() {
    if (dead) return;
    dead = true;

    // TODO(ray) Wouldn't it make more sense to tear down the children first?
    // And perhaps we shouldn't actually mark this scope dead until it's children
    // have died first. If we do that, take some care that re-entrant calls
    // to destroy() don't lead to redundant onExitScope calls. Maybe need an
    // enum State {LIVE, DYING, DEAD}.

    for (Scoped s : tearDowns) s.onExitScope();
    tearDowns.clear();
    services.clear();
    if (parent != null) parent.children.remove(getName());

    List<MortarScope> snapshot = new ArrayList<>(children.values());
    for (MortarScope child : snapshot) child.destroy();
  }

  @Override public String toString() {
    return "MortarScope@" + toHexString(System.identityHashCode(this)) + "{" +
        "name='" + getName() + '\'' +
        '}';
  }

  void assertNotDead() {
    if (isDestroyed()) throw new IllegalStateException("Scope " + getName() + " was destroyed");
  }

  public final static class Builder {
    private final String name;
    private final MortarScope parent;
    private final Map<String, Object> serviceProviders = new LinkedHashMap<>();

    Builder(String name, MortarScope parent) {
      this.name = name;
      this.parent = parent;
    }

    /**
     * Makes this service available via the new scope's {@link MortarScope#findService}
     * method. If the service implements {@link Scoped} it will be registered with
     * the new scope.
     */
    public Builder withService(String serviceName, Object service) {
      Object existing = serviceProviders.put(serviceName, service);
      if (existing != null) {
        throw new IllegalArgumentException(
            format("New scope \"%s\" already bound to service %s, cannot be rebound to %s", name,
                existing, service));
      }
      return this;
    }

    public MortarScope build() {
      MortarScope newScope = new MortarScope(name, parent, serviceProviders);
      if (parent != null) {
        parent.children.put(name, newScope);
      }

      for (Object service : serviceProviders.values()) {
        if (service instanceof Scoped) newScope.register((Scoped) service);
      }
      return newScope;
    }
  }
}
