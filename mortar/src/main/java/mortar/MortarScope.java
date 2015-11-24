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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;

public class MortarScope {
  public static final String DIVIDER = ">>>";

  private static final String MORTAR_SERVICE = MortarScope.class.getName();

  /**
   * Retrieves a MortarScope from the given context. If none is found, retrieves a MortarScope from
   * the application context.
   *
   * @return null if no scope is found in either the given context or the application context
   */
  public static MortarScope getScope(Context context) {
    //noinspection ResourceType
    Object scope = context.getSystemService(MORTAR_SERVICE);
    if (scope == null) {
      // Essentially a workaround for the lifecycle interval where an Activity's
      // base context is not yet set to the Application, but the Application
      // context is available and contains a MortarScope that provides needed
      // services. Thanks, Android!

      //noinspection ResourceType
      scope = context.getApplicationContext().getSystemService(MORTAR_SERVICE);
    }
    return (MortarScope) scope;
  }

  public static MortarScope findChild(Context context, String name) {
    return getScope(context).findChild(name);
  }

  public static Builder buildChild(Context context) {
    return getScope(context).buildChild();
  }

  public static boolean isDestroyed(Context context) {
    return getScope(context).isDestroyed();
  }

  public static Builder buildRootScope() {
    return new Builder(null);
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

  /**
   * Returns true if the service associated with the given name is provided by mortar.
   * It is safe to call this method on destroyed scopes.
   */
  public boolean hasService(String serviceName) {
    return serviceName.equals(MORTAR_SERVICE) || findService(serviceName, false) != null;
  }

  /**
   * Returns the service associated with the given name.
   *
   * @throws IllegalArgumentException if no such service can be found
   * @throws IllegalStateException if this scope is dead
   * @see #hasService
   */
  public <T> T getService(String serviceName) {
    T service = findService(serviceName, true);
    if (service == null) {
      throw new IllegalArgumentException(format("No service found named \"%s\"", serviceName));
    }

    return service;
  }

  @SuppressWarnings("unchecked") //
  private <T> T findService(String serviceName, boolean strict) {
    // Always honor requests for the scope itself, even if we're destroyed.
    // Otherwise things like if (MortarScope.getScope(context).isDestroyed()) are impossible.
    if (MORTAR_SERVICE.equals(serviceName)) return (T) this;

    if (strict) {
      assertNotDead();
    }

    T service = (T) services.get(serviceName);
    if (service != null) return service;

    if (parent != null) {
      return parent.findService(serviceName, strict);
    }

    return null;
  }

  /**
   * Find the scope from the root of the hierarchy, in which the scoped object is registered.
   */
  private MortarScope searchFromRoot(Scoped scoped)
  {
    // Ascend to the root.
    MortarScope root = this;
    while (root.parent != null) {
      root = root.parent;
    }

    // Do the non-recursive search.
    List<MortarScope> scopes = new LinkedList<>();
    scopes.add(root);

    while (!scopes.isEmpty()) {
      // Check first scope in the list.
      MortarScope scope = scopes.get(0);

      if (scope.tearDowns.contains(scoped)) {
        return scope;
      }

      // Replace the first scope with its children (breadth-first search).
      scopes.addAll(scope.children.values());
      scopes.remove(0);
    }

    return null;
  }

  /**
   * Register the given {@link Scoped} instance to have its {@link Scoped#onEnterScope(MortarScope)}
   * and {@link Scoped#onExitScope()} methods called. Redundant registrations are safe,
   * they will not lead to additional calls to these two methods.
   * <p>
   * {@link Scoped#onEnterScope(MortarScope) onEnterScope} is called synchronously.
   *
   * @throws IllegalStateException if this scope has been destroyed, or if the scoped object
   * is already registered with another scope in the same scope hierarchy.
   */
  public void register(Scoped scoped) {
    assertNotDead();
    if (tearDowns.contains(scoped)) {
      // Ignore redundant registrations.
      return;
    }

    MortarScope scope = searchFromRoot(scoped);
    if (scope != null) {
      throw new IllegalStateException(
          format("\"%s\" is already registered within \"%s\".", scoped, scope));
    }

    tearDowns.add(scoped);
    scoped.onEnterScope(this);
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

  public Builder buildChild() {
    assertNotDead();
    return new Builder(this);
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
   * Sends {@link Scoped#onExitScope()} to all registrants. Parent scope drops its reference to
   * this instance. Prior to this, recursively destroys all children. Redundant calls to this method
   * are safe.
   */
  public void destroy() {
    if (dead) return;
    dead = true;

    List<MortarScope> snapshot = new ArrayList<>(children.values());
    for (MortarScope child : snapshot) {
      child.destroy();
    }

    for (Scoped s : tearDowns) {
      s.onExitScope();
    }
    tearDowns.clear();

    Set<String> keys = services.keySet();
    for (String key : keys) {
      services.put(key, "Dead service");
    }
    if (parent != null) {
      parent.children.remove(getName());
    }
  }

  @Override public String toString() {
    return "MortarScope@" + toHexString(System.identityHashCode(this)) + "{" +
        "name='" + getName() + '\'' +
        '}';
  }

  void assertNotDead() {
    if (isDestroyed()) throw new IllegalStateException("Scope " + getName() + " was destroyed");
  }

  public static final class Builder {
    private final MortarScope parent;
    private final Map<String, Object> serviceProviders = new LinkedHashMap<>();

    Builder(MortarScope parent) {
      this.parent = parent;
    }

    /**
     * Makes this service available via the new scope's {@link MortarScope#findService}
     * method.
     */
    public Builder withService(String serviceName, Object service) {
      if (service instanceof Scoped) {
        throw new IllegalArgumentException(format(
            "For service %s, %s must not be an instance of %s, use \"withScopedService\" instead.",
            serviceName, service, Scoped.class.getSimpleName()));
      }
      return doWithService(serviceName, service);
    }

    /**
     * Makes this service available via the new scope's {@link MortarScope#findService}
     * method, and {@link MortarScope#register(Scoped) registers} it with the new scope.
     * Allows set up and tear down.
     */
    public Builder withService(String serviceName, Scoped service) {
      return doWithService(serviceName, service);
    }

    public MortarScope build(String name) {

      if (name.contains(DIVIDER)) {
        throw new IllegalArgumentException(
            format("Name \"%s\" must not contain '%s'", name, DIVIDER));
      }

      MortarScope newScope = new MortarScope(name, parent, serviceProviders);
      if (parent != null) {
        if (parent.children.containsKey(name)) {
          throw new IllegalArgumentException(
              format("Scope \"%s\" already has a child named \"%s\"", name, name));
        }

        parent.children.put(name, newScope);
      }

      for (Object service : serviceProviders.values()) {
        if (service instanceof Scoped) newScope.register((Scoped) service);
      }
      return newScope;
    }

    private Builder doWithService(String serviceName, Object service) {
      Object existing = serviceProviders.put(serviceName, service);
      if (service == null) {
        throw new NullPointerException("service == null");
      }
      if (existing != null) {
        throw new IllegalArgumentException(format(
            "Scope builder already bound \"%s\" to service \"%s\", cannot be rebound to \"%s\"",
            serviceName, existing.getClass().getName(), service.getClass().getName()));
      }
      return this;
    }
  }
}
