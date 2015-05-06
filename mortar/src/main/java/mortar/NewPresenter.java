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

public abstract class NewPresenter<V> {
  private V view = null;

  private Scoped registration = new Scoped() {
    @Override public void onEnterScope(MortarScope scope) {
      NewPresenter.this.onEnterScope(scope);
    }

    @Override public void onExitScope() {
      NewPresenter.this.onExitScope();
    }
  };

  /**
   * Called to give this presenter control of a view, e.g. from
   * {@link android.view.View#onAttachedToWindow()}. (Redundant calls will safely no-op.)
   * <p/>
   * It is expected that {@link #dropView(Object)} will be called with the same argument when the
   * view is no longer active, e.g. from {@link android.view.View#onDetachedFromWindow()}.
   */
  public final void takeView(V view) {
    if (view == null) throw new NullPointerException("new view must not be null");

    if (this.view != view) {
      if (this.view != null) {
        dropView(this.view);
      }

      this.view = view;
      extractScope(view).register(registration);
      NewPresenter.this.onTakeView(view);
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
    if (view == null) {
      throw new NullPointerException("dropped view must not be null");
    }
    if (view == this.view) {
      this.view = null;
      NewPresenter.this.onDropView();
    }
  }

  /** Called by {@link #takeView}. Given a view instance, return its {@link MortarScope}. */
  protected abstract MortarScope extractScope(V view);

  protected void onEnterScope(MortarScope scope) {
  }

  protected void onTakeView(V view) {
  }

  protected void onDropView() {
  }

  protected void onExitScope() {
  }
}
