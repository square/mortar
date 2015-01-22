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

class RealScope implements MortarScope {

  protected final Map<String, RealScope> children = new LinkedHashMap<>();

  protected boolean dead;

  private final Set<Scoped> tearDowns = new HashSet<>();
  private final Object seed;
  final RealScope parent;
  private final String name;
  private final Map<String, MortarServiceProvider> serviceProviders;

  RealScope(String name, RealScope parent, Object seed,
      Map<String, MortarServiceProvider> serviceProviders) {
    this.seed = seed;
    this.parent = parent;
    this.name = name;
    this.serviceProviders = serviceProviders;
  }

  @Override public final String getName() {
    return name;
  }

  @Override public String getPath() {
    if (parent == null) return getName();
    return parent.getPath() + ":" + getName();
  }

  @Override public Object getSeed() {
    return seed;
  }

  @Override public Object getService(String serviceName) {
    assertNotDead();

    if (MortarScope.class.getName().equals(serviceName)) return this;

    MortarServiceProvider serviceProvider = serviceProviders.get(serviceName);
    if (serviceProvider != null) return serviceProvider.getService(this);

    if (parent != null) return parent.getService(serviceName);

    return null;
  }

  @Override public Object getServiceProvider(String serviceName) {
    assertNotDead();
    return serviceProviders.get(serviceName);
  }

  @Override public void register(Scoped scoped) {
    assertNotDead();
    if (tearDowns.add(scoped)) scoped.onEnterScope(this);
  }

  @Override public RealScope findChild(String childName) {
    assertNotDead();
    return children.get(childName);
  }

  @Override public Builder buildChild(String childName) {
    assertNotDead();

    if (childName.contains(DIVIDER)) {
      throw new IllegalArgumentException(format("Name \"%s\" must not contain '%s'",
          childName, DIVIDER));
    }

    if (children.containsKey(childName)) {
      throw new IllegalArgumentException(
          format("Scope \"%s\" already has a child named \"%s\"", name, childName));
    }

    return new Builder(childName, this);
  }

  @Override public Context createContext(Context parentContext) {
    return new MortarContextWrapper(parentContext, this);
  }

  @Override public boolean isDestroyed() {
    return dead;
  }

  @Override public void destroy() {
    if (dead) return;
    dead = true;

    // TODO(ray) Wouldn't it make more sense to tear down the children first?
    // And perhaps we shouldn't actually mark this scope dead until it's children
    // have died first. If we do that, take some care that re-entrant calls
    // to destroy() don't lead to redundant onExitScope calls. Maybe need an
    // enum State {LIVE, DYING, DEAD}.

    for (Scoped s : tearDowns) s.onExitScope();
    tearDowns.clear();
    if (parent != null) parent.children.remove(getName());

    List<RealScope> snapshot = new ArrayList<>(children.values());
    for (RealScope child : snapshot) child.destroy();
  }

  @Override public String toString() {
    return "RealScope@" + toHexString(System.identityHashCode(this)) + "{" +
        "name='" + getName() + '\'' +
        '}';
  }

  boolean isDead() {
    return dead;
  }

  void assertNotDead() {
    if (isDead()) throw new IllegalStateException("Scope " + getName() + " was destroyed");
  }
}
