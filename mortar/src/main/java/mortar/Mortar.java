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
import dagger.ObjectGraph;

import static java.lang.String.format;
import static mortar.MortarContextWrapper.MORTAR_SCOPE_SERVICE;

/** Provides static bootstrap and integration methods. */
public class Mortar {

  private Mortar() {
  }

  /**
   * Creates a core scope to live above the activity level, typically an app-wide singleton managed
   * by a custom {@link android.app.Application}.
   */
  public static MortarScope createRootScope(boolean validate) {
    return new RealMortarScope(validate, ObjectGraph.create());
  }

  /**
   * Creates a core scope to live above the activity level, typically an app-wide singleton managed
   * by a custom {@link android.app.Application}.
   */
  public static MortarScope createRootScope(boolean validate, ObjectGraph objectGraph) {
    return new RealMortarScope(validate, objectGraph);
  }

  /**
   * Returns the existing {@link MortarActivityScope} scope for the given {@link Activity}, or
   * uses the {@link Blueprint} to create one if none is found.
   * <p/>
   * It is expected that this method will be called from {@link Activity#onCreate}. Calling
   * it at other times may lead to surprises.
   */
  public static MortarActivityScope requireActivityScope(MortarScope parentScope,
      final Blueprint blueprint) {
    String name = blueprint.getMortarScopeName();
    RealMortarScope unwrapped = (RealMortarScope) parentScope.requireChild(blueprint);

    RealActivityScope activityScope;
    if (unwrapped instanceof MortarActivityScope) {
      activityScope = (RealActivityScope) unwrapped;
    } else {
      RealMortarScope realParentScope = (RealMortarScope) parentScope;
      activityScope = new RealActivityScope(unwrapped);
      realParentScope.replaceChild(name, activityScope);
    }

    return activityScope;
  }

  /**
   * A convenience wrapper for {@link #getScope} to simplify dynamic injection, typically
   * for {@link Activity} and {@link android.view.View} instances that must be instantiated
   * by Android.
   */
  public static void inject(Context context, Object object) {
    getScope(context).getObjectGraph().inject(object);
  }

  /**
   * Find the scope for the given {@link Context}.
   *
   * @see MortarScope#createContext(android.content.Context)
   */
  public static MortarScope getScope(Context context) {
    MortarScope scope = (MortarScope) context.getSystemService(MORTAR_SCOPE_SERVICE);
    if (scope == null) {
      throw new IllegalArgumentException(
          format("Cannot find scope in %s.  Make sure your Activity's content view is set using "
              + " an inflater from MortarScope.createContext()", context.getClass().getName()));
    }
    return scope;
  }
}
