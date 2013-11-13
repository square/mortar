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
import java.util.Iterator;
import java.util.Set;

import static java.lang.String.format;

class RealActivityScope extends RealMortarScope implements MortarActivityScope {
  private final Set<Bundler> unloaded = new HashSet<Bundler>();
  protected final Set<Bundler> loaded = new HashSet<Bundler>();

  private boolean active;
  protected Bundle latestSavedInstanceState;

  protected RealActivityScope(RealMortarScope original, boolean active) {
    super(original.getName(), ((RealMortarScope) original.getParent()), original.validate,
        original.getObjectGraph());
    this.active = active;
  }

  @Override public void register(Scoped scoped) {
    doRegister(scoped);
    if (!(scoped instanceof Bundler)) return;

    Bundler b = (Bundler) scoped;
    String mortarBundleKey = b.getMortarBundleKey();
    if (mortarBundleKey == null || mortarBundleKey.trim().equals("")) {
      throw new IllegalArgumentException(format("%s has null or empty bundle key", b));
    }

    if (loaded.contains(b) || unloaded.contains(b)) return;

    if (active) {
      b.onLoad(getChildBundle(b, latestSavedInstanceState));
      loaded.add(b);
    } else {
      unloaded.add(b);
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    // Make note of the bundle to send it to bundlers when register is called.
    latestSavedInstanceState = savedInstanceState;

    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) continue;
      ((RealActivityScope) child).onCreate(savedInstanceState);
    }
  }

  @Override public void onResume() {
    while (!unloaded.isEmpty()) {
      Iterator<Bundler> i = unloaded.iterator();
      while (i.hasNext()) {
        Bundler b = i.next();
        i.remove();
        b.onLoad(getChildBundle(b, latestSavedInstanceState));
        loaded.add(b);
      }
    }
    active = true;

    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) continue;
      ((RealActivityScope) child).onResume();
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) continue;
      ((RealActivityScope) child).onSaveInstanceState(outState);
    }

    active = false;

    latestSavedInstanceState = outState;
    for (Bundler b : loaded) {
      b.onSave(getChildBundle(b, outState));
      unloaded.add(b);
    }
    loaded.clear();
  }

  @Override public MortarScope requireChild(Blueprint blueprint) {
    MortarScope unwrapped = super.requireChild(blueprint);
    if (unwrapped instanceof RealActivityScope) return unwrapped;

    RealActivityScope childScope = new RealActivityScope((RealMortarScope) unwrapped, active);
    replaceChild(blueprint.getMortarScopeName(), childScope);
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
