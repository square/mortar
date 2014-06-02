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
import dagger.ObjectGraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;

class RealScope implements MortarScope {

  protected final boolean validate;
  protected final Map<String, RealScope> children = new LinkedHashMap<>();

  protected boolean dead;

  private final Set<Scoped> tearDowns = new HashSet<>();
  private final ObjectGraph graph;
  private final RealScope parent;
  private final String name;

  RealScope(boolean validate, ObjectGraph objectGraph) {
    this(MortarScope.ROOT_NAME, null, validate, objectGraph);
  }

  RealScope(String name, RealScope parent, boolean validate, ObjectGraph graph) {
    this.graph = graph;
    this.parent = parent;
    this.name = name;
    this.validate = validate;

    if (validate) graph.validate();
  }

  @Override public final String getName() {
    return name;
  }

  @Override public final ObjectGraph getObjectGraph() {
    assertNotDead();
    return graph;
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

  boolean isRoot() {
    return parent == null;
  }

  @Override public RealScope findChild(String childName) {
    assertNotDead();
    return children.get(childName);
  }

  @Override
  public MortarScope requireChild(Blueprint blueprint) {
    assertNotDead();

    String childName = blueprint.getMortarScopeName();
    RealScope child = findChild(childName);

    if (child == null) {
      Object daggerModule = blueprint.getDaggerModule();
      ObjectGraph newGraph;
      if (daggerModule == null) {
        newGraph = graph.plus();
      } else if (daggerModule instanceof Collection) {
        Collection c = (Collection) daggerModule;
        newGraph = graph.plus(c.toArray(new Object[c.size()]));
      } else {
        newGraph = graph.plus(daggerModule);
      }
      child = new RealScope(childName, this, validate, newGraph);
      children.put(childName, child);
    }

    return child;
  }

  @Override public Context createContext(Context parentContext) {
    return new MortarContextWrapper(parentContext, this);
  }

  @Override public void destroyChild(MortarScope child) {
    RealScope realChild = (RealScope) child;
    if (realChild.parent != this) {
      throw new IllegalArgumentException(format("%s was created by another scope", name));
    }

    realChild.doDestroy();
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

  void doDestroy() {
    if (dead) return;
    dead = true;

    for (Scoped s : tearDowns) s.onExitScope();
    tearDowns.clear();
    if (parent != null) parent.onChildDestroyed(this);

    List<RealScope> snapshot = new ArrayList<>(children.values());
    for (RealScope child : snapshot) child.doDestroy();
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
