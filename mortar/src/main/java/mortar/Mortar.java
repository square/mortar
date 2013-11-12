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

public class Mortar {

  private Mortar() {
  }

  /**
   * Creates the root scope, which must be an app-wide singleton. It is typically managed by an
   * {@link android.app.Application} implementing {@link HasMortarScope}.
   */
  public static MortarScope createRootScope(boolean validate, Object... modules) {
    return new RealMortarScope(MortarScope.ROOT_NAME, null, validate, ObjectGraph.create(modules));
  }

  /**
   * Returns the existing {@link MortarActivityScope} scope for the given {@link Activity}, or
   * uses the {@link Blueprint} to create one if none is found.
   */
  public static MortarActivityScope getActivityScope(MortarScope parentScope,
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

  public static void inject(Context context, Object object) {
    getScope(context).getObjectGraph().inject(object);
  }

  public static MortarScope getScope(Context context) {
    return ((HasMortarScope) context).getMortarScope();
  }
}
