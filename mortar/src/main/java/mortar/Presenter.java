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
import java.lang.ref.WeakReference;

public abstract class Presenter<V> implements Bundler {
  /**
   * Views are not required to detach themselves, because Android makes it very difficult
   * to know when to do so (impossible on Gingerbread). And even if it were practical, we can't
   * rely on every single view implementation remembering to do that bit of bookkeeping. Therefore
   * we keep a weak reference to the view. If everyone else has forgotten about it, the presenter
   * will too.
   */
  private WeakReference<V> view = new WeakReference<V>(null);

  /**
   * Called to give this presenter control of a view. Do not call this from the view's
   * constructor. Instead call it after construction when the view is known to be going
   * live, e.g. from {@link android.app.Activity#onCreate} or
   * {@link android.view.View#onAttachedToWindow()}.
   * <p/>
   * This presenter will be immediately {@link MortarActivityScope#register registered} (or
   * re-registered), leading to an immediate call to {@link #onLoad}
   * <p/>
   * The presenter will retain the view in a {@link WeakReference}.
   *
   * @see MortarActivityScope#register
   */
  public void takeView(V view) {
    if (view == null) throw new NullPointerException("view must not be null");
    this.view = new WeakReference<V>(view);
    extractScope(view).register(this);
  }

  protected abstract MortarScope extractScope(V view);

  /**
   * Returns the view managed by this presenter, or null if the view has never been set or has been
   * garbage collected.
   */
  protected final V getView() {
    return view.get();
  }

  /**
   * Called to explicitly surrender control of this view, e.g. when a dialog is
   * dismissed.
   */
  protected final void dropView() {
    view.clear();
  }

  @Override public String getMortarBundleKey() {
    return getClass().getName();
  }

  @Override public void onLoad(Bundle savedInstanceState) {
  }

  @Override public void onSave(Bundle outState) {
  }

  @Override public void onDestroy() {
    dropView();
  }
}
