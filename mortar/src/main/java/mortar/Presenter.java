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

public abstract class Presenter<V> {
  private V view = null;

  /** Load has been called for the current {@link #view}. */
  private boolean loaded;

  private Bundler registration = new Bundler() {
    @Override public String getMortarBundleKey() {
      return Presenter.this.getMortarBundleKey();
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      if (getView() != null && !loaded) {
        loaded = true;
        Presenter.this.onLoad(savedInstanceState);
      }
    }

    @Override public void onSave(Bundle outState) {
      Presenter.this.onSave(outState);
    }

    @Override public void onEnterScope(MortarScope scope) {
      Presenter.this.onEnterScope(scope);
    }

    @Override public void onExitScope() {
      Presenter.this.onExitScope();
    }
  };

  /**
   * Called to give this presenter control of a view, ideally from {@link
   * android.view.View#onFinishInflate}. If a view is to be re-used, make an additional call from
   * {@link android.view.View#onAttachedToWindow()}. (Redundant calls will safely no-op.) Sets the
   * view that will be returned from {@link #getView()}.
   * <p/>
   * This presenter will be immediately {@link MortarActivityScope#register registered}
   * (or re-registered) with the given view's scope, leading to an immediate call to {@link
   * #onLoad}.
   * <p/>
   * It is expected that {@link #dropView(Object)} will be called with the same argument when the
   * view is no longer active, e.g. from {@link android.view.View#onDetachedFromWindow()}.
   *
   * @see MortarActivityScope#register
   */
  public final void takeView(V view) {
    if (view == null) throw new NullPointerException("new view must not be null");

    if (this.view != view) {
      if (this.view != null) dropView(this.view);

      this.view = view;
      extractScope(view).register(registration);
    }
  }

  /**
   * Called to surrender control of this view, e.g. when the view is detached. If and only if
   * the given view matches the last passed to {@link #takeView}, the reference to the view is
   * cleared.
   * <p/>
   * Mismatched views are a no-op, not an error. This is to provide protection in the
   * not uncommon case that dropView and takeView are called out of order. For example, an
   * activity's views are typically inflated in {@link
   * android.app.Activity#onCreate}, but are only detached some time after {@link
   * android.app.Activity#onDestroy() onExitScope}. It's possible for a view from one activity
   * to be detached well after the window for the next activity has its views inflated&mdash;that
   * is, after the next activity's onResume call.
   */
  public void dropView(V view) {
    if (view == null) throw new NullPointerException("dropped view must not be null");
    if (view == this.view) {
      loaded = false;
      this.view = null;
    }
  }

  protected String getMortarBundleKey() {
    return getClass().getName();
  }

  /** Called by {@link #takeView}. Given a view instance, return its {@link MortarScope}. */
  protected abstract MortarScope extractScope(V view);

  /**
   * Returns the view managed by this presenter, or null if {@link #takeView} has never been
   * called, or after {@link #dropView}.
   */
  protected final V getView() {
    return view;
  }

  /** Like {@link Bundler#onEnterScope}. */
  protected void onEnterScope(MortarScope scope) {
  }

  /**
   * Like {@link Bundler#onLoad}, but called only when {@link #getView()} is not
   * null, and debounced. That is, this method will be called exactly once for a given view
   * instance, at least until that view is {@link #dropView(Object) dropped}.
   *
   * See {@link #takeView} for details.
   */
  protected void onLoad(Bundle savedInstanceState) {
  }

  /** Like {@link Bundler#onSave}. */
  protected void onSave(Bundle outState) {
  }

  /**
   * Like {@link Bundler#onExitScope}. One subtlety to note is that a presenter may be created
   * by a higher level scope than the one it is registered with, in which case it may receive
   * multiple calls to this method.
   */
  protected void onExitScope() {
  }
}
