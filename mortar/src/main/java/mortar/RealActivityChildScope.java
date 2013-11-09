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

import static mortar.RealActivityScope.getChildBundle;

class RealActivityChildScope extends RealMortarScope implements Bundler {
  private final Set<Bundler> bundlers = new HashSet<Bundler>();
  private Bundle latestSavedInstanceState;

  RealActivityChildScope(RealMortarScope original) {
    super(original.getName(), ((RealMortarScope) original.getParent()), original.validate,
        original.getObjectGraph());
  }

  @Override public void register(Scoped scoped) {
    if (scoped instanceof Bundler) {
      Bundler b = (Bundler) scoped;
      b.onLoad(getChildBundle(b, latestSavedInstanceState));
      if (!bundlers.contains(b)) bundlers.add(b);
    }

    doRegister(scoped);
  }

  void onCreate(Bundle savedInstanceState) {
    this.latestSavedInstanceState = savedInstanceState;
    for (Bundler b : bundlers) {
      if (b instanceof RealActivityChildScope) {
        ((RealActivityChildScope) b).onCreate(getChildBundle(b, savedInstanceState));
      }
    }
  }

  @Override public String getMortarBundleKey() {
    return getName();
  }

  @Override public void onLoad(Bundle savedInstanceState) {
    this.latestSavedInstanceState = savedInstanceState;
    for (Bundler b : bundlers) b.onLoad(getChildBundle(b, savedInstanceState));
  }

  @Override public void onSave(Bundle outState) {
    for (Bundler b : bundlers) b.onSave(getChildBundle(b, outState));
  }

  @Override void onChildDestroyed(RealMortarScope child) {
    if (latestSavedInstanceState != null) latestSavedInstanceState.putBundle(child.getName(), null);
    super.onChildDestroyed(child);
  }

  @Override public void onDestroy() {
  }

  @Override public MortarScope requireChild(BluePrint bluePrint) {
    RealMortarScope unwrapped = (RealMortarScope) super.requireChild(bluePrint);
    RealActivityChildScope childScope = new RealActivityChildScope(unwrapped);
    replaceChild(bluePrint.getMortarScopeName(), childScope);
    register(childScope);
    return childScope;
  }
}
