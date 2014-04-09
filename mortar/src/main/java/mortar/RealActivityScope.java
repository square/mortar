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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

class RealActivityScope extends RealMortarScope implements MortarActivityScope {
  private Bundle latestSavedInstanceState;

  private enum LoadingState {
    IDLE, LOADING, SAVING
  }

  private LoadingState myLoadingState = LoadingState.IDLE;

  private List<Bundler> toloadThisTime = new ArrayList<Bundler>();
  private Set<Bundler> bundlers = new HashSet<Bundler>();

  RealActivityScope(RealMortarScope original) {
    super(original.getName(), original.getParent(), original.validate, original.getObjectGraph());
  }

  @Override public void register(Scoped scoped) {
    doRegister(scoped);
    if (!(scoped instanceof Bundler)) return;

    Bundler b = (Bundler) scoped;
    String mortarBundleKey = b.getMortarBundleKey();
    if (mortarBundleKey == null || mortarBundleKey.trim().equals("")) {
      throw new IllegalArgumentException(format("%s has null or empty bundle key", b));
    }

    LoadingState loadingState = effectiveLoadingState(this);

    switch (loadingState) {
      case IDLE:
        toloadThisTime.add(b);
        doLoading();
        break;
      case LOADING:
        if (!toloadThisTime.contains(b)) toloadThisTime.add(b);
        break;
      case SAVING:
        bundlers.add(b);
        break;

      default:
        throw new AssertionError("Unknown state " + loadingState);
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    assertNotDead();

    // Make note of the bundle to send it to bundlers when register is called.
    latestSavedInstanceState = savedInstanceState;

    toloadThisTime.addAll(bundlers);
    if (effectiveLoadingState(this) == LoadingState.IDLE) doLoading();

    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) continue;
      ((RealActivityScope) child).onCreate(getChildBundle(child, savedInstanceState, false));
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    assertNotDead();
    if (myLoadingState != LoadingState.IDLE) {
      throw new IllegalStateException("Cannot handle onSaveInstanceState while " + myLoadingState);
    }

    latestSavedInstanceState = outState;

    myLoadingState = LoadingState.SAVING;
    for (Bundler b : new ArrayList<Bundler>(bundlers)) {
      // If anyone's onSave method destroyed us, short circuit.
      if (isDead()) return;

      b.onSave(getChildBundle(b, latestSavedInstanceState, true));
    }

    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) return;
      ((RealActivityScope) child).onSaveInstanceState(
          getChildBundle(child, latestSavedInstanceState, true));
    }

    myLoadingState = LoadingState.IDLE;
  }

  @Override public MortarScope requireChild(Blueprint blueprint) {
    MortarScope unwrapped = super.requireChild(blueprint);
    if (unwrapped instanceof RealActivityScope) return unwrapped;

    RealActivityScope childScope = new RealActivityScope((RealMortarScope) unwrapped);
    replaceChild(blueprint.getMortarScopeName(), childScope);
    childScope.onCreate(getChildBundle(childScope, latestSavedInstanceState, false));

    return childScope;
  }

  @Override void onChildDestroyed(RealMortarScope child) {
    if (latestSavedInstanceState != null) {
      String name = child.getName();
      latestSavedInstanceState.putBundle(name, null);
    }
    super.onChildDestroyed(child);
  }

  private void doLoading() {
    if (myLoadingState != LoadingState.IDLE && myLoadingState != LoadingState.LOADING) {
      throw new IllegalStateException("Cannot load while " + myLoadingState);
    }

    // Call onLoad. Watch out for new registrants, and don't loop on re-registration.
    // Also watch out for the scope getting destroyed from an onload, short circuit.

    myLoadingState = LoadingState.LOADING;
    while (!toloadThisTime.isEmpty()) {
      if (isDead()) return;

      Bundler next = toloadThisTime.remove(0);
      bundlers.add(next);
      next.onLoad(getChildBundle(next, latestSavedInstanceState, false));
    }
    myLoadingState = LoadingState.IDLE;

    for (RealMortarScope child : children.values()) {
      if (child instanceof RealActivityScope) ((RealActivityScope) child).doLoading();
    }
  }

  private Bundle getChildBundle(Bundler bundler, Bundle bundle, boolean eager) {
    return getNamedBundle(bundler.getMortarBundleKey(), bundle, eager);
  }

  private Bundle getChildBundle(MortarScope scope, Bundle bundle, boolean eager) {
    return getNamedBundle(scope.getName(), bundle, eager);
  }

  private Bundle getNamedBundle(String name, Bundle bundle, boolean eager) {
    if (bundle == null) return null;

    Bundle child = bundle.getBundle(name);
    if (eager && child == null) {
      child = new Bundle();
      bundle.putBundle(name, child);
    }
    return child;
  }

  private LoadingState effectiveLoadingState(RealActivityScope realActivityScope) {
    LoadingState s = realActivityScope.myLoadingState;
    if (s != LoadingState.IDLE) {
      return s;
    }

    RealMortarScope parent = realActivityScope.getParent();
    if (!(parent instanceof RealActivityScope)) return s;

    return effectiveLoadingState((RealActivityScope) parent);
  }
}
