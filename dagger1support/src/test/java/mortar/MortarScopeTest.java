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

import android.content.Context;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import java.lang.annotation.Retention;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Qualifier;
import mortar.dagger1support.Dagger1;
import mortar.dagger1support.Blueprint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static dagger.ObjectGraph.create;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static mortar.Mortar.MORTAR_SCOPE_SERVICE;
import static mortar.dagger1support.Dagger1.requireChild;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("InnerClassMayBeStatic") public class MortarScopeTest {

  @Mock Context context;
  @Mock Scoped scoped;

  @Qualifier @Retention(RUNTIME) @interface Apple {
  }

  @Qualifier @Retention(RUNTIME) @interface Bagel {
  }

  @Qualifier @Retention(RUNTIME) @interface Carrot {
  }

  @Qualifier @Retention(RUNTIME) @interface Dogfood {
  }

  @Qualifier @Retention(RUNTIME) @interface Eggplant {
  }

  @Module(injects = HasApple.class) class Able {
    @Provides @Apple String provideApple() {
      return Apple.class.getName();
    }
  }

  class AbleBlueprint implements Blueprint {
    @Override public String getMortarScopeName() {
      return Apple.class.getName();
    }

    @Override public Object getDaggerModule() {
      return new Able();
    }
  }

  @Module(injects = HasBagel.class) class Baker {
    @Provides @Bagel String provideBagel() {
      return Bagel.class.getName();
    }
  }

  class BakerBlueprint implements Blueprint {
    @Override public String getMortarScopeName() {
      return Bagel.class.getName();
    }

    @Override public Object getDaggerModule() {
      return new Baker();
    }
  }

  @Module(injects = HasCarrot.class) class Charlie {
    @Provides @Carrot String provideCharlie() {
      return Carrot.class.getName();
    }
  }

  class CharlieBlueprint implements Blueprint {
    @Override public String getMortarScopeName() {
      return Carrot.class.getName();
    }

    @Override public Object getDaggerModule() {
      return new Charlie();
    }
  }

  @Module(injects = HasDogfood.class) class Delta {
    @Provides @Dogfood String provideDogfood() {
      return Dogfood.class.getName();
    }
  }

  class DeltaBlueprint implements Blueprint {
    @Override public String getMortarScopeName() {
      return Dogfood.class.getName();
    }

    @Override public Object getDaggerModule() {
      return new Delta();
    }
  }

  @Module(injects = HasEggplant.class) class Echo {
    @Provides @Eggplant String provideEggplant() {
      return Eggplant.class.getName();
    }
  }

  class MoreModules implements Blueprint {

    @Override public String getMortarScopeName() {
      return "Moar";
    }

    @Override public Object getDaggerModule() {
      return asList(new Delta(), new Echo());
    }
  }

  class NoModules implements Blueprint {

    @Override public String getMortarScopeName() {
      return "Nothing";
    }

    @Override public Object getDaggerModule() {
      return null;
    }
  }

  static class HasApple {
    @Inject @Apple String string;
  }

  static class HasBagel {
    @Inject @Bagel String string;
  }

  static class HasCarrot {
    @Inject @Carrot String string;
  }

  static class HasDogfood {
    @Inject @Dogfood String string;
  }

  static class HasEggplant {
    @Inject @Eggplant String string;
  }

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void MortarScopeHasName() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    assertThat(scope.getName()).isSameAs(MortarScope.ROOT_NAME);
  }

  @Test public void createMortarScopeUsesModules() {
    MortarScope scope = Mortar.createRootScope(create(new Able(), new Baker()));
    ObjectGraph objectGraph = scope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void destroyRoot() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    scope.register(scoped);
    Mortar.destroyRootScope(scope);
    verify(scoped).onExitScope();
  }

  @Test public void activityScopeName() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());

    String bagel = Bagel.class.getName();
    assertThat(activityScope.getName()).isEqualTo(bagel);
    assertThat(root.findChild(bagel)).isSameAs(activityScope);
    assertThat(requireChild(root, new BakerBlueprint())).isSameAs(activityScope);
    assertThat(Dagger1.requireActivityScope(root, new BakerBlueprint())).isSameAs(activityScope);
    assertThat(root.findChild("herman")).isNull();
  }

  @Test public void getActivityScopeWithOneModule() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    ObjectGraph objectGraph = activityScope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void getActivityScopeWithMoreModules() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new MoreModules());
    ObjectGraph objectGraph = activityScope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void destroyActivityScopeDirect() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    BakerBlueprint blueprint = new BakerBlueprint();
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    root.destroyChild(activityScope);
    verify(scoped).onExitScope();
    assertThat(root.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test public void destroyActivityScopeRecursive() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    BakerBlueprint blueprint = new BakerBlueprint();
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    Mortar.destroyRootScope(root);
    verify(scoped).onExitScope();
    try {
      activityScope.getObjectGraph();
      fail("Expected IllegalStateException from destroyed child");
    } catch (IllegalStateException e) {
      // pass;
    }
  }

  @Test public void activityChildScopeName() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());

    String carrot = Carrot.class.getName();
    assertThat(child.getName()).isEqualTo(carrot);
    assertThat(activityScope.findChild(carrot)).isSameAs(child);
    assertThat(requireChild(activityScope, new CharlieBlueprint())).isSameAs(child);
    assertThat(activityScope.findChild("herman")).isNull();
  }

  @Test public void requireGrandchildWithOneModule() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new DeltaBlueprint());
    ObjectGraph objectGraph = grandchild.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    try {
      objectGraph.get(HasEggplant.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void requireGrandchildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new MoreModules());

    ObjectGraph objectGraph = grandchild.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(String.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void requireGrandchildWithNoModules() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new NoModules());

    ObjectGraph objectGraph = grandchild.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());

    try {
      objectGraph.get(String.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test public void cannotDestroyAnothersChild() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    CharlieBlueprint blueprint = new CharlieBlueprint();
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, blueprint);

    try {
      root.destroyChild(child);
      fail("Expected exception");
    } catch (IllegalArgumentException e) {
      // ta da!
    }
  }

  @Test public void destroyActivityChildScopeDirect() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    CharlieBlueprint blueprint = new CharlieBlueprint();
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    activityScope.destroyChild(child);
    verify(scoped).onExitScope();
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test public void destroyActivityChildScopeRecursive() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    CharlieBlueprint blueprint = new CharlieBlueprint();
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    Mortar.destroyRootScope(root);
    verify(scoped).onExitScope();
    try {
      child.getObjectGraph();
      fail("Expected IllegalStateException from destroyed child");
    } catch (IllegalStateException e) {
      // pass;
    }
  }

  @Test public void activityGrandchildScopeName() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new DeltaBlueprint());

    String dogfood = Dogfood.class.getName();
    assertThat(grandchild.getName()).isEqualTo(dogfood);
    assertThat(child.findChild(dogfood)).isSameAs(grandchild);
    assertThat(requireChild(child, new DeltaBlueprint())).isSameAs(grandchild);
    assertThat(child.findChild("herman")).isNull();
  }

  @Test public void requireChildWithOneModule() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
  }

  @Test public void requireChildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarActivityScope activityScope = Dagger1.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new MoreModules());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
  }

  @Test public void requireChildWithNoModules() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarScope child = requireChild(root, new NoModules());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
  }

  @Test public void handlesRecursiveDestroy() {
    final AtomicInteger i = new AtomicInteger(0);

    final MortarScope scope = Mortar.createRootScope(create(new Able()));
    scope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        i.incrementAndGet();
        Mortar.destroyRootScope(scope);
      }
    });
    Mortar.destroyRootScope(scope);
    assertThat(i.get()).isEqualTo(1);
  }

  @Test public void inject() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    when(context.getSystemService(MORTAR_SCOPE_SERVICE)).thenReturn(root);
    HasApple apple = new HasApple();
    Mortar.getScope(context).<ObjectGraph>getObjectGraph().inject(apple);
    assertThat(apple.string).isEqualTo(Apple.class.getName());
  }

  @Test public void getScope() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    when(context.getSystemService(MORTAR_SCOPE_SERVICE)).thenReturn(root);
    assertThat(Mortar.getScope(context)).isSameAs(root);
  }

  @Test public void canGetNameFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(scope);
    assertThat(scope.getName()).isEqualTo(MortarScope.ROOT_NAME);
  }

  @Test(expected = IllegalStateException.class) public void cannotGetObjectGraphFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(scope);
    scope.getObjectGraph();
  }

  @Test(expected = IllegalStateException.class) public void cannotRegisterOnDestroyed() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(scope);
    scope.register(scoped);
  }

  @Test(expected = IllegalStateException.class) public void cannotFindChildFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(scope);
    scope.findChild("foo");
  }

  @Test(expected = IllegalStateException.class) public void cannotRequireChildFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(scope);
    requireChild(scope, new AbleBlueprint());
  }

  @Test public void destroyIsIdempotent() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    MortarScope child = requireChild(root, new NoModules());

    final AtomicInteger destroys = new AtomicInteger(0);
    child.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        destroys.addAndGet(1);
      }
    });

    root.destroyChild(child);
    assertThat(destroys.get()).isEqualTo(1);

    root.destroyChild(child);
    assertThat(destroys.get()).isEqualTo(1);
  }

  @Test public void rootDestroyIsIdempotent() {
    MortarScope scope = Mortar.createRootScope(create(new Able()));

    final AtomicInteger destroys = new AtomicInteger(0);
    scope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        destroys.addAndGet(1);
      }
    });

    Mortar.destroyRootScope(scope);
    assertThat(destroys.get()).isEqualTo(1);

    Mortar.destroyRootScope(scope);
    assertThat(destroys.get()).isEqualTo(1);
  }

  @Test public void isDestroyedStartsFalse() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    assertThat(root.isDestroyed()).isFalse();
  }

  @Test public void isDestroyedGetsSet() {
    MortarScope root = Mortar.createRootScope(create(new Able()));
    Mortar.destroyRootScope(root);
    assertThat(root.isDestroyed()).isTrue();
  }
}
