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
import dagger.Module;
import dagger.ObjectGraph;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import mortar.dagger1support.Dagger1Blueprint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static mortar.Mortar.createRootScope;
import static mortar.Mortar.requireActivityScope;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class MortarActivityScopeTest {

  private static class MyBundler implements Bundler {
    final String name;

    MortarScope registered;
    boolean loaded;
    Bundle lastLoaded;
    Bundle lastSaved;
    boolean destroyed;

    public MyBundler(String name) {
      this.name = name;
    }

    void reset() {
      lastSaved = lastLoaded = null;
      loaded = destroyed = false;
    }

    @Override public String getMortarBundleKey() {
      return name;
    }

    @Override public void onEnterScope(MortarScope scope) {
      this.registered = scope;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      loaded = true;
      lastLoaded = savedInstanceState;
      if (savedInstanceState != null) {
        assertThat(savedInstanceState.get("key")).isEqualTo(name);
      }
    }

    @Override public void onSave(Bundle outState) {
      lastSaved = outState;
      outState.putString("key", name);
    }

    @Override public void onExitScope() {
      destroyed = true;
    }
  }

  static class MyBlueprint extends Dagger1Blueprint {
    private final String name;

    MyBlueprint(String name) {
      this.name = name;
    }

    @Override public String getMortarScopeName() {
      return name;
    }

    @Override public Object getDaggerModule() {
      return new MyModule();
    }
  }

  @Module static class MyModule {
  }

  @Mock Scoped scoped;

  private MortarScope root;
  private MortarActivityScope activityScope;

  @Before
  public void setUp() {
    initMocks(this);
    resetScope();
  }

  private void resetScope() {
    root = createRootScope(ObjectGraph.create(new MyModule()));
    activityScope = requireActivityScope(root, new MyBlueprint("activity"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonNullKeyRequired() {
    activityScope.register(mock(Bundler.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonEmptyKeyRequired() {
    Bundler mock = mock(Bundler.class);
    when(mock.getMortarBundleKey()).thenReturn("");
    activityScope.register(mock);
  }

  @Test
  public void lifeCycle() {
    doLifecycleTest(activityScope);
  }

  @Test
  public void childLifeCycle() {
    doLifecycleTest(activityScope.requireChild(new MyBlueprint("child")));
  }

  private void doLifecycleTest(MortarScope registerScope) {
    MyBundler able = new MyBundler("able");
    MyBundler baker = new MyBundler("baker");

    registerScope.register(scoped);
    registerScope.register(able);
    registerScope.register(baker);

    // onEnterScope is called immediately.
    verify(scoped).onEnterScope(registerScope);
    assertThat(able.registered).isSameAs(registerScope);
    assertThat(baker.registered).isSameAs(registerScope);

    // Load is called immediately.
    assertThat(able.loaded).isTrue();
    assertThat(able.lastLoaded).isNull();
    able.reset();
    assertThat(baker.loaded).isTrue();
    assertThat(baker.lastLoaded).isNull();
    baker.reset();

    activityScope.onCreate(null);
    // Create loads all registrants.
    assertThat(able.loaded).isTrue();
    assertThat(able.lastLoaded).isNull();
    able.reset();
    assertThat(baker.loaded).isTrue();
    assertThat(baker.lastLoaded).isNull();
    baker.reset();

    // When we save, the bundler gets its own bundle to write to.
    Bundle saved = new Bundle();
    activityScope.onSaveInstanceState(saved);
    assertThat(able.lastSaved).isNotNull();
    assertThat(baker.lastSaved).isNotNull();
    assertThat(able.lastSaved).isNotSameAs(baker.lastSaved);

    // If the bundler is re-registered, it loads again.
    able.lastLoaded = null;
    registerScope.register(able);
    assertThat(able.lastLoaded).isSameAs(able.lastSaved);

    // A new activity instance appears
    able.reset();
    baker.reset();
    activityScope.onSaveInstanceState(saved);
    Bundle fromNewActivity = new Bundle(saved);

    activityScope.onCreate(fromNewActivity);
    assertThat(able.lastLoaded).isNotNull();

    verifyNoMoreInteractions(scoped);

    root.destroyChild(activityScope);
    assertThat(able.destroyed).isTrue();
    verify(scoped).onExitScope();
  }

  class FauxActivity {
    final MyBundler rootBundler = new MyBundler("core");

    MortarScope childScope;
    MyBundler childBundler = new MyBundler("child");

    void create(Bundle bundle) {
      activityScope.onCreate(bundle);
      activityScope.register(rootBundler);
      childScope = activityScope.requireChild(new MyBlueprint("child"));
      childScope.register(childBundler);
    }
  }

  @Test public void onRegisteredIsDebounced() {
    activityScope.register(scoped);
    activityScope.register(scoped);
    verify(scoped, times(1)).onEnterScope(activityScope);
  }

  @Test public void childInfoSurvivesProcessDeath() {
    FauxActivity activity = new FauxActivity();
    activity.create(null);
    Bundle bundle = new Bundle();
    activityScope.onSaveInstanceState(bundle);

    // Process death: new copy of the bundle, new scope and activity instances
    bundle = new Bundle(bundle);
    resetScope();
    activity = new FauxActivity();
    activity.create(bundle);
    assertThat(activity.rootBundler.lastLoaded).isNotNull();
    assertThat(activity.childBundler.lastLoaded).isNotNull();
  }

  @Test public void handlesRegisterFromOnLoadBeforeCreate() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: a create call reloads it.
    bundler.reset();
    activityScope.onCreate(null);

    assertThat(bundler.loaded).isTrue();
  }

  @Test public void handlesRegisterFromOnLoadAfterCreate() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.onCreate(null);

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: the next create call reloads it.
    bundler.reset();
    Bundle b = new Bundle();
    activityScope.onSaveInstanceState(b);
    activityScope.onCreate(b);

    assertThat(bundler.loaded).isNotNull();
  }

  @Test public void cannotRegisterDuringOnSave() {
    final MyBundler bundler = new MyBundler("inner");
    final AtomicBoolean caught = new AtomicBoolean(false);

    activityScope.onCreate(null);

    activityScope.register(new MyBundler("outer") {
      @Override public void onSave(Bundle outState) {
        super.onSave(outState);
        try {
          activityScope.register(bundler);
        } catch (IllegalStateException e) {
          caught.set(true);
        }
      }
    });
    assertThat(bundler.loaded).isFalse();

    Bundle bundle = new Bundle();
    activityScope.onSaveInstanceState(bundle);
    assertThat(caught.get()).isTrue();
  }

  @Test public void handlesReregistrationBeforeCreate() {
    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (i.incrementAndGet() < 1) activityScope.register(this);
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onExitScope() {
        throw new UnsupportedOperationException();
      }
    });

    Bundle b = new Bundle();
    activityScope.onCreate(b);

    assertThat(i.get()).isEqualTo(2);
  }

  @Test public void handlesReregistrationAfterCreate() {
    Bundle b = new Bundle();
    activityScope.onCreate(b);

    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (i.incrementAndGet() < 1) activityScope.register(this);
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onExitScope() {
        throw new UnsupportedOperationException();
      }
    });

    assertThat(i.get()).isEqualTo(1);
  }

  @Test
  public void handleDestroyFromEarlyLoad() {
    final AtomicInteger loads = new AtomicInteger(0);
    final AtomicInteger destroys = new AtomicInteger(0);

    class Destroyer implements Bundler {
      @Override public String getMortarBundleKey() {
        return "k";
      }

      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (loads.incrementAndGet() > 2) {
          root.destroyChild(activityScope);
        }
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onExitScope() {
        destroys.incrementAndGet();
      }
    }

    activityScope.register(new Destroyer());
    activityScope.register(new Destroyer());

    Bundle b = new Bundle();
    activityScope.onCreate(b);

    assertThat(loads.get()).isEqualTo(3);
    assertThat(destroys.get()).isEqualTo(2);
  }

  @Test
  public void handlesDestroyFromOnSave() {
    final AtomicInteger saves = new AtomicInteger(0);
    final AtomicInteger destroys = new AtomicInteger(0);

    class Destroyer implements Bundler {
      @Override public String getMortarBundleKey() {
        return "k";
      }

      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onLoad(Bundle savedInstanceState) {
      }

      @Override public void onSave(Bundle outState) {
        saves.incrementAndGet();
        root.destroyChild(activityScope);
      }

      @Override public void onExitScope() {
        destroys.incrementAndGet();
      }
    }

    activityScope.register(new Destroyer());
    activityScope.register(new Destroyer());

    Bundle b = new Bundle();
    activityScope.onCreate(b);
    activityScope.onSaveInstanceState(b);

    assertThat(saves.get()).isEqualTo(1);
    assertThat(destroys.get()).isEqualTo(2);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotOnCreateDestroyed() {
    root.destroyChild(activityScope);
    activityScope.onCreate(null);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotOnSaveDestroyed() {
    root.destroyChild(activityScope);
    activityScope.onSaveInstanceState(new Bundle());
  }

  @Test
  public void deliversStateToBundlerWhenRegisterAfterOnCreate() {
    MyBundler bundler = new MyBundler("bundler");
    Bundle bundlerState = new Bundle();
    bundler.onSave(bundlerState);
    Bundle scopeState = new Bundle();
    scopeState.putBundle(bundler.name, bundlerState);

    activityScope.onCreate(scopeState);
    activityScope.register(bundler);

    assertThat(bundler.lastLoaded).isSameAs(bundlerState);
  }

  @Test
  public void deliversChildScopeStateWhenRequireChildDuringRegisterAfterOnCreate() {
    final MyBundler childScopeBundler = new MyBundler("childScopeBundler");
    final MyBlueprint childScopeBlueprint = new MyBlueprint("ChildScope");

    // When that bundler is loaded, it creates a child scope and register a bundler on it.
    MyBundler activityScopeBundler = new MyBundler("activityScopeBundler") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        MortarScope childScope = activityScope.requireChild(childScopeBlueprint);
        childScope.register(childScopeBundler);
      }
    };

    Bundle childScopeBundlerState = new Bundle();
    childScopeBundler.onSave(childScopeBundlerState);

    Bundle childScopeState = new Bundle();
    childScopeState.putBundle(childScopeBundler.name, childScopeBundlerState);

    Bundle activityScopeBundlerState = new Bundle();
    activityScopeBundler.onSave(activityScopeBundlerState);

    Bundle activityScopeState = new Bundle();
    activityScopeState.putBundle(childScopeBlueprint.getMortarScopeName(), childScopeState);
    activityScopeState.putBundle(activityScopeBundler.name, activityScopeBundlerState);

    // activityScope doesn't have any child scope or Bundler yet.
    activityScope.onCreate(activityScopeState);

    // Loads activityScopeBundler which require a child on activityScope and add a bundler to it.
    activityScope.register(activityScopeBundler);

    assertThat(childScopeBundler.lastLoaded).isSameAs(childScopeBundlerState);
  }

  /** <a href="https://github.com/square/mortar/issues/46">Issue 46</a> */
  @Test
  public void registerWithDescendantScopesCreatedDuringParentOnCreateGetOnlyOneOnLoadCall() {
    final MyBundler childBundler = new MyBundler("child");
    final MyBundler grandChildBundler = new MyBundler("grandChild");

    final AtomicBoolean spawnSubScope = new AtomicBoolean(false);

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        if (spawnSubScope.get()) {
          MortarScope childScope =
              activityScope.requireChild(new MyBlueprint("child scope"));
          childScope.register(childBundler);
          // 1. We're in the middle of loading, so the usual register > load call doesn't happen.
          assertThat(childBundler.loaded).isFalse();

          childScope.requireChild(new MyBlueprint("grandchild scope"))
              .register(grandChildBundler);
          assertThat(grandChildBundler.loaded).isFalse();
        }
      }
    });

    spawnSubScope.set(true);
    activityScope.onCreate(null);

    // 2. But load is called before the onCreate chain ends.
    assertThat(childBundler.loaded).isTrue();
    assertThat(grandChildBundler.loaded).isTrue();
  }

  @Test public void peerBundlersLoadSynchronouslyButThoseInChildScopesShouldWait() {
    final MyBundler peerBundler = new MyBundler("bro");
    final MyBundler childBundler = new MyBundler("child");
    final MyBundler grandchildBundler = new MyBundler("grandchild");

    final MortarScope childScope =
        activityScope.requireChild(new MyBlueprint("child scope"));
    final MortarScope grandChildScope =
        childScope.requireChild(new MyBlueprint("grandchild scope"));

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.register(peerBundler);
        assertThat(peerBundler.loaded).isTrue();

        childScope.register(childBundler);
        assertThat(childBundler.loaded).isFalse();

        grandChildScope.register(grandchildBundler);
        assertThat(grandchildBundler.loaded).isFalse();
      }
    });

    assertThat(childBundler.loaded).isTrue();
    assertThat(grandchildBundler.loaded).isTrue();
  }

  @Test
  public void peerBundlersLoadSynchronouslyButThoseInChildScopesShouldWaitEvenInAFreshScope() {
    final MyBundler peerBundler = new MyBundler("bro");
    final MyBundler childBundler = new MyBundler("child");
    final MyBundler grandchildBundler = new MyBundler("grandchild");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.register(peerBundler);
        assertThat(peerBundler.loaded).isTrue();

        MortarScope childScope = activityScope.requireChild(new MyBlueprint("child scope"));
        childScope.register(childBundler);
        assertThat(childBundler.loaded).isFalse();

        MortarScope grandchildScope = childScope.requireChild(
            new MyBlueprint("grandchild scope"));
        grandchildScope.register(grandchildBundler);
        assertThat(grandchildBundler.loaded).isFalse();
      }
    });

    assertThat(childBundler.loaded).isTrue();
    assertThat(grandchildBundler.loaded).isTrue();
  }

  /**
   * Happened during first naive fix of
   * <a href="https://github.com/square/mortar/issues/46">Issue 46</a>.
   */
  @Test
  public void descendantScopesCreatedDuringParentOnLoadAreNotStuckInLoadingMode() {
    final MyBlueprint subscopeBlueprint = new MyBlueprint("subscope");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.requireChild(subscopeBlueprint).requireChild(subscopeBlueprint);
      }
    });

    activityScope.onSaveInstanceState(new Bundle());
    // No crash? Victoire!
  }

  /**
   * https://github.com/square/mortar/issues/77
   */
  @Test
  public void childCreatedDuringMyLoadDoesLoadingAfterMe() {
    activityScope.onCreate(null);
    final MyBundler childBundler = new MyBundler("childBundler");

    activityScope.register(new MyBundler("root") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);

        activityScope.requireChild(new MyBlueprint("childScope")).register(childBundler);
        assertThat(childBundler.loaded).isFalse();
      }
    });

    assertThat(childBundler.loaded).isTrue();
  }

  /**
   * https://github.com/square/mortar/issues/77
   */
  @Test
  public void bundlersInChildScopesLoadAfterBundlersOnParent() {
    activityScope.onCreate(null);
    final MyBundler service = new MyBundler("service");

    final MyBundler childBundler = new MyBundler("childBundler") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        assertThat(service.loaded).isTrue();
      }
    };

    activityScope.register(new MyBundler("root") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.requireChild(new MyBlueprint("childScope")).register(childBundler);
        assertThat(childBundler.loaded).isFalse();

        activityScope.register(service);
        assertThat(service.loaded).isTrue();
      }
    });
    assertThat(childBundler.loaded).isTrue();
  }
}
