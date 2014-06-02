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

public interface MortarScope {
  String ROOT_NAME = "Root";

  /** Returns the name of this scope. */
  String getName();

  /**
   * Returns the {@link ObjectGraph} for this scope.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  ObjectGraph getObjectGraph();

  /**
   * Register the given {@link Scoped} instance to have its {@link Scoped#onExitScope()}
   * method called from {@link MortarScope#destroy}. Redundant calls are safe, they will not lead
   * to double registration.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  void register(Scoped scoped);

  /**
   * Returns the child instance whose name matches the given, or null if there is none.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  MortarScope findChild(String name);

  /**
   * Returns the existing child whose name matches the given {@link Blueprint}'s
   * {@link Blueprint#getMortarScopeName()} value. If there is none, a new child is created
   * based on {@link Blueprint#getDaggerModule()}. Note that {@link Blueprint#getDaggerModule()} is
   * not called otherwise.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  MortarScope requireChild(Blueprint blueprint);

  /**
   * Creates a new Context based on the given parent and this scope. e.g.:
   * <pre><code>
   * MortarScope childScope = getMortarScope.requireChild(new ChildBlueprint());
   * MyView newChildView = new MyView(childScope.createContext(getContext());
   * </code></pre>
   */
  Context createContext(Context parentContext);

  /**
   * Sends {@link Scoped#onExitScope()} to all registrants and then clears the
   * registration list. Recursively destroys all children. Parent scope drops its reference
   * to this instance. Redundant calls to this method are safe.
   */
  void destroyChild(MortarScope child);

  /** Returns true if this scope has been destroyed, false otherwise. */
  boolean isDestroyed();
}
