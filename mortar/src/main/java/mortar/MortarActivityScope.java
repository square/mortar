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

/**
 * A specialized scope that provides access to the Android Activity life cycle's persistence
 * bundle.
 */
public interface MortarActivityScope extends MortarScope {
  /**
   * <p>Extends {@link MortarScope#register(Scoped)} to register {@link Bundler} instances to have
   * {@link Bundler#onLoad} and {@link Bundler#onSave} called from {@link #onCreate} and {@link
   * #onSaveInstanceState}, respectively.
   *
   * <p>In addition to the calls from {@link #onCreate}, {@link Bundler#onLoad} is triggered by
   * registration. In most cases that initial {@link Bundler#onLoad} is made synchronously during
   * registration. However, if a {@link Bundler} is registered while an ancestor scope is loading
   * its own {@link Bundler}s, its {@link Bundler#onLoad} will be deferred until all ancestor
   * scopes have completed loading. This ensures that a {@link Bundler} can assume that any
   * dependency registered with a higher-level scope will have been initialized before its own
   * {@link Bundler#onLoad} method fires.
   *
   * <p>A redundant call to this method does not create a duplicate registration, but does trigger
   * another call to {@link Bundler#onLoad}.
   */
  @Override void register(Scoped s);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Calls the registered {@link Bundler}'s {@link Bundler#onLoad}
   * methods. To avoid redundant calls to {@link Presenter#onLoad} it's best to call this before
   * {@link android.app.Activity#setContentView}.
   */
  void onCreate(Bundle savedInstanceState);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onSaveInstanceState}. Calls the registrants' {@link Bundler#onSave}
   * methods.
   */
  void onSaveInstanceState(Bundle outState);
}
