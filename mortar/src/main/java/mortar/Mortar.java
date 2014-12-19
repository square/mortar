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

import android.app.Activity;
import android.content.Context;

import static java.lang.String.format;

/** Provides static bootstrap and integration methods. */
public class Mortar {

  static final String MORTAR_SCOPE_SERVICE = "mortar_scope";

  private Mortar() {
  }

  /**
   * Creates a core scope to live above the activity level, typically an app-wide singleton managed
   * by a custom {@link android.app.Application}.
   */
  public static MortarScope createRootScope(Object objectGraph) {
    return new RealScope(objectGraph);
  }

  /**
   * Creates a {@link MortarActivityScope} child scope from an application scope. Used to forward
   * lifecycle callbacks to child scopes.
   *
   *  <p/>
   * It is expected that this method will be called from {@link Activity#onCreate}. Calling
   * it at other times may lead to surprises.
   * <p/>
   * This scope can be destroyed by the {@link MortarScope#destroyChild} method on the
   * given parent.
   */
  public static MortarActivityScope createActivityScope(MortarScope parentScope, String childName,
      Object childGraph) {
    RealScope unwrapped = (RealScope) parentScope.createChild(childName, childGraph);
    RealActivityScope activityScope;
    RealScope realParentScope = (RealScope) parentScope;
    activityScope = new RealActivityScope(unwrapped);
    realParentScope.replaceChild(childName, activityScope);
    return activityScope;
  }

  /**
   * Destroys a scope previously created by {@link Mortar#createRootScope(Object)}.
   */
  public static void destroyRootScope(MortarScope rootScope) {
    RealScope realScope = (RealScope) rootScope;
    if (!realScope.isRoot()) {
      throw new IllegalArgumentException(String.format("%s is not a root", realScope.getName()));
    }
    realScope.doDestroy();
  }

  /**
   * Find the scope for the given {@link Context}.
   *
   * @see MortarScope#createContext(android.content.Context)
   */
  public static MortarScope getScope(Context context) {
    //noinspection unchecked
    MortarScope scope = (MortarScope) context.getSystemService(MORTAR_SCOPE_SERVICE);
    if (scope == null) {
      throw new IllegalArgumentException(format(
          "Cannot find scope in %s. Make sure your Activity overrides getSystemService() "
              + " to return its scope if isScopeSystemService() is true",
          context.getClass().getName()));
    }
    return scope;
  }

  /**
   * Use this when overriding {@link android.app.Activity#getSystemService(String)}. If this
   * returns true, you should return the activity scope from there.
   */
  public static boolean isScopeSystemService(String name) {
    return MORTAR_SCOPE_SERVICE.equals(name);
  }
}
