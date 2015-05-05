/*
* Copyright 2014 Square Inc.
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
import mortar.bundler.BundleService;
import mortar.bundler.BundleServiceRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PresenterTest {
  static class SomeView {
  }

  MortarScope root;
  MortarScope activityScope;

  @Before public void setUp() {
    root = MortarScope.buildRootScope().build("Root");
    activityScope = root.buildChild()
        .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
        .build("name");
  }

  class ChildPresenter extends Presenter<SomeView> {
    final String payload;
    boolean loaded;

    ChildPresenter(String payload) {
      this.payload = payload;
    }

    @Override protected BundleService extractBundleService(SomeView view) {
      return BundleService.getBundleService(activityScope);
    }

    @Override protected void onSave(Bundle savedInstanceState) {
      savedInstanceState.putString("key", payload);
    }

    @Override protected void onLoad(Bundle savedInstanceState) {
      if (savedInstanceState != null) {
        assertThat(savedInstanceState.getString("key")).isEqualTo(payload);
        loaded = true;
      }
    }
  }

  class ParentPresenter extends Presenter<SomeView> {
    @Override protected BundleService extractBundleService(SomeView view) {
      return BundleService.getBundleService(activityScope);
    }

    // The child presenters are anonymous inner classes but of the same
    // type. This is like the case where a presenter manages two
    // popup presenters for two different dialogs.

    ChildPresenter childOne = new ChildPresenter("one") {
    };
    ChildPresenter childTwo = new ChildPresenter("two") {
    };

    @Override protected void onLoad(Bundle savedInstanceState) {
      childOne.takeView(getView());
      childTwo.takeView(getView());
    }

    @Override public void dropView(SomeView view) {
      childTwo.dropView(view);
      childOne.dropView(view);
      super.dropView(view);
    }
  }

  @Test public void childPresentersGetTheirOwnBundles() {
    BundleServiceRunner bundleServiceRunner =
        BundleServiceRunner.getBundleServiceRunner(activityScope);
    bundleServiceRunner.onCreate(null);

    ParentPresenter presenter = new ParentPresenter();
    SomeView view = new SomeView();

    presenter.takeView(view);

    Bundle bundle = new Bundle();
    bundleServiceRunner.onSaveInstanceState(bundle);
    presenter.dropView(view);

    bundleServiceRunner.onCreate(bundle);
    presenter.takeView(view);

    /**
     * Assertions in {@link ChildPresenter#onLoad(android.os.Bundle)} are the real test,
     * but let's check that the were run
     */

    assertThat(presenter.childOne.loaded).isTrue();
    assertThat(presenter.childTwo.loaded).isTrue();
  }

  class SimplePresenter extends Presenter<SomeView> {
    MortarScope registered;
    MortarScope destroyed;
    boolean loaded;
    Object droppedView;

    @Override protected void onEnterScope(MortarScope scope) {
      registered = scope;
    }

    @Override protected BundleService extractBundleService(SomeView view) {
      return BundleService.getBundleService(activityScope);
    }

    @Override protected void onLoad(Bundle savedInstanceState) {
      loaded = true;
    }

    @Override public void dropView(SomeView view) {
      droppedView = view;
      super.dropView(view);
    }

    @Override protected void onExitScope() {
      destroyed = activityScope;
    }
  }

  /** https://github.com/square/mortar/issues/59 */
  @Test public void onLoadOnlyOncePerView() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView view = new SomeView();

    presenter.takeView(view);
    assertThat(presenter.loaded).isTrue();

    presenter.loaded = false;
    BundleServiceRunner.getBundleServiceRunner(activityScope).onCreate(null);
    assertThat(presenter.loaded).isFalse();
  }

  @Test public void newViewNewLoad() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView viewOne = new SomeView();

    presenter.takeView(viewOne);
    assertThat(presenter.loaded).isTrue();

    presenter.loaded = false;
    SomeView viewTwo = new SomeView();
    presenter.takeView(viewTwo);
    assertThat(presenter.loaded).isTrue();
  }

  @Test public void dropRetakeReload() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView view = new SomeView();

    presenter.takeView(view);
    assertThat(presenter.loaded).isTrue();

    presenter.dropView(view);

    presenter.loaded = false;
    presenter.takeView(view);
    assertThat(presenter.loaded).isTrue();
  }

  /**
   * When takeView clobbers an existing view, dropView should be called. (We could
   * drop this requirement if dropView were final, see https://github.com/square/mortar/issues/52)
   */
  @Test public void autoDropCallsDrop() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView viewOne = new SomeView();
    SomeView viewTwo = new SomeView();

    presenter.takeView(viewOne);
    presenter.takeView(viewTwo);
    assertThat(presenter.droppedView).isSameAs(viewOne);
  }

  @Test public void onRegisteredIsFired() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView viewOne = new SomeView();

    presenter.takeView(viewOne);
    assertThat(presenter.registered).isSameAs(activityScope);
  }

  @Test public void onRegisteredIsDebounced() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView viewOne = new SomeView();

    presenter.takeView(viewOne);
    presenter.dropView(viewOne);
    presenter.registered = null;

    presenter.takeView(viewOne);
    assertThat(presenter.registered).isNull();
  }

  @Test public void onExitIsFired() {
    SimplePresenter presenter = new SimplePresenter();
    SomeView viewOne = new SomeView();

    presenter.takeView(viewOne);
    activityScope.destroy();

    assertThat(presenter.destroyed).isSameAs(activityScope);
  }
}
