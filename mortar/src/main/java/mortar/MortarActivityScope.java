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
   * <li>{@link Bundler#onLoad} will also be called from {@link #onCreate}
   * <li>{@link Bundler#onSave} is called from {@link #onSaveInstanceState}
   * </ul>
   */
  @Override void register(Scoped s);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Calls the registrants' {@link Bundler#onLoad}
   * methods. Makes note of the {@link Bundle} to be used for later calls to {@link #register}.
   * To avoid redundant calls to {@link Presenter#onLoad} it's best to call this before
   * {@link android.app.Activity#setContentView}.
   */
  void onCreate(Bundle savedInstanceState);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onSaveInstanceState}. Calls the registrants' {@link Bundler#onSave}
   * methods. Makes note of the {@link Bundle} to be used for later calls to {@link #register}.
   */
  void onSaveInstanceState(Bundle outState);
}
