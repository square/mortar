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

import android.os.Bundle;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

class RealActivityScope extends RealMortarScope implements MortarActivityScope {
  private final Set<Bundler> bundlers = new HashSet<Bundler>();

  private Bundle latestSavedInstanceState;

  RealActivityScope(RealMortarScope original) {
    super(original.getName(), ((RealMortarScope) original.getParent()), original.validate,
        original.getObjectGraph());
  }

  @Override public void register(Scoped scoped) {
    if (scoped instanceof Bundler) {
      Bundler b = (Bundler) scoped;
      String mortarBundleKey = b.getMortarBundleKey();
      if (mortarBundleKey == null || mortarBundleKey.trim().equals("")) {
        throw new IllegalArgumentException(format("%s has null or empty bundle key", b));
      }
      b.onLoad(getChildBundle(b, latestSavedInstanceState));
      if (!bundlers.contains(b)) bundlers.add(b);
    }

    doRegister(scoped);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    latestSavedInstanceState = savedInstanceState;
    for (Bundler b : bundlers) {
      if (b instanceof RealActivityChildScope) {
        ((RealActivityChildScope) b).onCreate(getChildBundle(b, savedInstanceState));
      }
    }
  }

  @Override public void onResume() {
    for (Bundler b : bundlers) b.onLoad(getChildBundle(b, latestSavedInstanceState));
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    latestSavedInstanceState = outState;
    for (Bundler b : bundlers) b.onSave(getChildBundle(b, outState));
  }

  @Override public MortarScope requireChild(Blueprint blueprint) {
    MortarScope unwrapped = super.requireChild(blueprint);
    if (unwrapped instanceof RealActivityChildScope) return unwrapped;

    RealActivityChildScope childScope = new RealActivityChildScope((RealMortarScope) unwrapped);
    replaceChild(blueprint.getMortarScopeName(), childScope);
    register(childScope);
    return childScope;
  }

  @Override void onChildDestroyed(RealMortarScope child) {
    if (latestSavedInstanceState != null) {
      String name = child.getName();
      latestSavedInstanceState.putBundle(name, null);
    }
    super.onChildDestroyed(child);
  }

  static Bundle getChildBundle(Bundler bundler, Bundle bundle) {
    return bundle == null ? null : getNamedBundle(bundler.getMortarBundleKey(), bundle);
  }

  private static Bundle getNamedBundle(String name, Bundle bundle) {
    Bundle child = bundle.getBundle(name);
    if (child == null) {
      child = new Bundle();
      bundle.putBundle(name, child);
    }
    return child;
  }
}
