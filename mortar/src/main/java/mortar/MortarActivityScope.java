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
   * Registers the given to have its {@link Scoped#onDestroy()} method called. In addition,
   * if it is an instance of {@link Bundler}:
   * <ul>
   * <li>{@link Bundler#onLoad} is called immediately</li>
   * <li>{@link Bundler#onLoad} is also called from {@link android.app.Activity#onResume()}
   * <li>{@link Bundler#onSave} is called from {@link android.app.Activity#onSaveInstanceState}
   * </ul>
   * Note well that calls to onLoad and onSave are not symmetric: it is par for the course to
   * receive redundant onLoad calls.
   */
  @Override void register(Scoped s);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Calls the registrants' {@link Bundler#onLoad} methods.
   * Makes note of the {@link Bundle} to be used for later registrations.
   */
  void onCreate(Bundle savedInstanceState);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onSaveInstanceState}. Calls the registrants' {@link Bundler#onSave}
   * methods
   */
  void onSaveInstanceState(Bundle outState);
}
