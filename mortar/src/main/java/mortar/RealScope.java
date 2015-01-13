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
  private final Object graph;
  private final RealScope parent;
  private final String name;

  RealScope(Object objectGraph) {
    this(MortarScope.ROOT_NAME, null, objectGraph);
  }

  RealScope(String name, RealScope parent, Object graph) {
    this.graph = graph;
    this.parent = parent;
    this.name = name;
  }

  @Override public final String getName() {
    return name;
  }

  @Override public final <T> T getObjectGraph() {
    assertNotDead();
    //noinspection unchecked
    return (T) graph;
  }

  @Override public void register(Scoped scoped) {
    if (scoped instanceof Bundler) {
      throw new IllegalArgumentException(format("Scope %s cannot register %s instance %s. "
              + "Only %ss and their children can provide bundle services", getName(),
          Bundler.class.getSimpleName(), ((Bundler) scoped).getMortarBundleKey(),
          MortarActivityScope.class.getSimpleName()));
    }

    doRegister(scoped);
  }

  void doRegister(Scoped scoped) {
    assertNotDead();
    if (tearDowns.add(scoped)) scoped.onEnterScope(this);
  }

  RealScope getParent() {
    return parent;
  }

  @Override public RealScope findChild(String childName) {
    assertNotDead();
    return children.get(childName);
  }

  @Override public MortarScope createChild(String childName, Object childObjectGraph) {
    assertNotDead();
    if (children.containsKey(childName)) {
      throw new IllegalArgumentException(name + " Scope already has a child named " + childName);
    }
    RealScope child = new RealScope(childName, this, childObjectGraph);
    children.put(childName, child);
    return child;
  }

  @Override public Context createContext(Context parentContext) {
    return new MortarContextWrapper(parentContext, this);
  }

  @Override public boolean isDestroyed() {
    return dead;
  }

  void replaceChild(String childName, RealScope scope) {
    if (scope.getParent() != this) {
      throw new IllegalArgumentException("Replacement scope must have receiver as parent");
    }
    children.put(childName, scope);
  }

  void onChildDestroyed(RealScope child) {
    children.remove(child.getName());
  }

  @Override public void destroy() {
    if (dead) return;
    dead = true;

    for (Scoped s : tearDowns) s.onExitScope();
    tearDowns.clear();
    if (parent != null) parent.onChildDestroyed(this);

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
