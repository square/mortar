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

  private boolean duringOnCreate;

  private enum LoadingState {
    IDLE, LOADING, SAVING
  }

  private LoadingState loadingState = LoadingState.IDLE;

  private List<Bundler> toloadThisTime = new ArrayList<Bundler>();
  private Set<Bundler> bundlers = new HashSet<Bundler>();

  RealActivityScope(RealMortarScope original) {
    this(original, false);
  }

  private RealActivityScope(RealMortarScope original, boolean duringParentOnCreate) {
    super(original.getName(), original.getParent(), original.validate, original.getObjectGraph());

    /**
     * If I was created while my parent is in {@link #doLoading()} from an {@link #onCreate} call,
     * I should wait for the call to my own {@link #onCreate} before making doing any loading
     * of my own.
     * https://github.com/square/mortar/issues/46
     */
    this.loadingState = duringParentOnCreate ? LoadingState.LOADING : LoadingState.IDLE;
  }

  @Override public void register(Scoped scoped) {
    doRegister(scoped);
    if (!(scoped instanceof Bundler)) return;

    Bundler b = (Bundler) scoped;
    String mortarBundleKey = b.getMortarBundleKey();
    if (mortarBundleKey == null || mortarBundleKey.trim().equals("")) {
      throw new IllegalArgumentException(format("%s has null or empty bundle key", b));
    }

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
    duringOnCreate = true;
    doLoading();
    duringOnCreate = false;

    for (RealMortarScope child : children.values()) {
      if (!(child instanceof RealActivityScope)) continue;
      ((RealActivityScope) child).onCreate(getChildBundle(child, savedInstanceState, false));
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    assertNotDead();
    if (loadingState != LoadingState.IDLE) {
      throw new IllegalStateException("Cannot handle onSaveInstanceState while " + loadingState);
    }

    latestSavedInstanceState = outState;

    loadingState = LoadingState.SAVING;
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

    loadingState = LoadingState.IDLE;
  }

  @Override public MortarScope requireChild(Blueprint blueprint) {
    MortarScope unwrapped = super.requireChild(blueprint);
    if (unwrapped instanceof RealActivityScope) return unwrapped;

    RealActivityScope childScope =
        new RealActivityScope((RealMortarScope) unwrapped, duringOnCreate);
    replaceChild(blueprint.getMortarScopeName(), childScope);
    if (loadingState != LoadingState.LOADING) {
      childScope.onCreate(getChildBundle(childScope, latestSavedInstanceState, false));
    }
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
    if (loadingState != LoadingState.IDLE && loadingState != LoadingState.LOADING) {
      throw new IllegalStateException("Cannot load while " + loadingState);
    }

    // Call onLoad. Watch out for new registrants, and don't loop on re-registration.
    // Also watch out for the scope getting destroyed from an onload, short circuit.

    loadingState = LoadingState.LOADING;
    while (!toloadThisTime.isEmpty()) {
      if (isDead()) return;

      Bundler next = toloadThisTime.remove(0);
      bundlers.add(next);
      next.onLoad(getChildBundle(next, latestSavedInstanceState, false));
    }
    loadingState = LoadingState.IDLE;
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
}
