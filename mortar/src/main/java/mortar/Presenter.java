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

public abstract class Presenter<V> implements Bundler {
  private V view = null;

  /**
   * Called to give this presenter control of a view. Do not call this from the view's
   * constructor. Instead call it after construction when the view is known to be going
   * live, e.g. from {@link android.view.View#onAttachedToWindow()} or from
   * {@link android.app.Activity#onCreate}.
   * <p/>
   * This presenter will be immediately {@link MortarActivityScope#register registered} (or
   * re-registered), leading to an immediate call to {@link #onLoad}. It is expected that
   * {@link #dropView(Object)} will be called with the same argument when the view is
   * no longer active, e.g. from {@link android.view.View#onAttachedToWindow()} or from
   * {@link android.app.Activity#onDestroy()}.
   *
   * @see MortarActivityScope#register
   */
  public void takeView(V view) {
    if (view == null) throw new NullPointerException("new view must not be null");
    this.view = view;
    extractScope(view).register(this);
  }

  /**
   * Called to surrender control of this view, e.g. when a dialog is dismissed. If and only if the
   * given view matches the last passed to {@link #takeView}, the reference to the view is
   * cleared. Mismatched views are a no-op, not an error. This is to provide protection in the
   * not uncommon case that dropView and takeView are called out of order.
   * <p/>
   * For example, an activity's views are attached after {@link android.app.Activity#onResume
   * onResume}, but are only detached some time after {@link android.app.Activity#onDestroy()
   * onDestroy}. It's possible for a view from one activity to be detached only after the window
   * for the next activity has its views attached&mdash;that is, after the next activity's onResume
   * call.
   */
  public void dropView(V view) {
    if (view == null) throw new NullPointerException("dropped view must not be null");
    if (view == this.view) this.view = null;
  }

  protected abstract MortarScope extractScope(V view);

  /**
   * Returns the view managed by this presenter, or null if the view has never been set or has been
   * {@link #dropView dropped}.
   */
  protected final V getView() {
    return view;
  }

  @Override public String getMortarBundleKey() {
    return getClass().getName();
  }

  @Override public void onLoad(Bundle savedInstanceState) {
  }

  @Override public void onSave(Bundle outState) {
  }

  @Override public void onDestroy() {
    this.view = null;
  }
}
