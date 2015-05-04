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
import mortar.dagger1support.Blueprint;
import mortar.dagger1support.ObjectGraphService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static dagger.ObjectGraph.create;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static mortar.dagger1support.ObjectGraphService.getObjectGraph;
import static mortar.dagger1support.ObjectGraphService.requireChild;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
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

  @Test public void createMortarScopeUsesModules() {
    MortarScope scope = createRootScope(create(new Able(), new Baker()));
    ObjectGraph objectGraph = getObjectGraph(scope);
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
    MortarScope scope = createRootScope(create(new Able()));
    scope.register(scoped);
    scope.destroy();
    verify(scoped).onExitScope();
  }

  @Test public void activityScopeName() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());

    String bagel = Bagel.class.getName();
    assertThat(activityScope.getName()).isEqualTo(bagel);
    assertThat(root.findChild(bagel)).isSameAs(activityScope);
    assertThat(requireChild(root, new BakerBlueprint())).isSameAs(activityScope);
    assertThat(ObjectGraphService.requireActivityScope(root, new BakerBlueprint())).isSameAs(
        activityScope);
    assertThat(root.findChild("herman")).isNull();
  }

  @Test public void getActivityScopeWithOneModule() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    ObjectGraph objectGraph = getObjectGraph(activityScope);
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
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new MoreModules());
    ObjectGraph objectGraph = getObjectGraph(activityScope);
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
    MortarScope root = createRootScope(create(new Able()));
    BakerBlueprint blueprint = new BakerBlueprint();
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    activityScope.destroy();
    verify(scoped).onExitScope();
    assertThat(root.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test public void destroyActivityScopeRecursive() {
    MortarScope root = createRootScope(create(new Able()));
    BakerBlueprint blueprint = new BakerBlueprint();
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    root.destroy();
    verify(scoped).onExitScope();

    IllegalStateException caught = null;
    try {
      getObjectGraph(activityScope);
    } catch (IllegalStateException e) {
      caught = e;
    }
    assertThat(caught).isNotNull();
  }

  @Test public void activityChildScopeName() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());

    String carrot = Carrot.class.getName();
    assertThat(child.getName()).isEqualTo(carrot);
    assertThat(activityScope.findChild(carrot)).isSameAs(child);
    assertThat(requireChild(activityScope, new CharlieBlueprint())).isSameAs(child);
    assertThat(activityScope.findChild("herman")).isNull();
  }

  @Test public void requireGrandchildWithOneModule() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new DeltaBlueprint());
    ObjectGraph objectGraph = getObjectGraph(grandchild);
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
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new MoreModules());

    ObjectGraph objectGraph = getObjectGraph(grandchild);
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
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new NoModules());

    ObjectGraph objectGraph = getObjectGraph(grandchild);
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

  @Test public void destroyActivityChildScopeDirect() {
    MortarScope root = createRootScope(create(new Able()));
    CharlieBlueprint blueprint = new CharlieBlueprint();
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    child.destroy();
    verify(scoped).onExitScope();
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test public void destroyActivityChildScopeRecursive() {
    MortarScope root = createRootScope(create(new Able()));
    CharlieBlueprint blueprint = new CharlieBlueprint();
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    root.destroy();
    verify(scoped).onExitScope();

    IllegalStateException caught = null;
    try {
      assertThat(getObjectGraph(child)).isNull();
    } catch (IllegalStateException e) {
      caught = e;
    }
    assertThat(caught).isNotNull();
  }

  @Test public void activityGrandchildScopeName() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());
    MortarScope grandchild = requireChild(child, new DeltaBlueprint());

    String dogfood = Dogfood.class.getName();
    assertThat(grandchild.getName()).isEqualTo(dogfood);
    assertThat(child.findChild(dogfood)).isSameAs(grandchild);
    assertThat(requireChild(child, new DeltaBlueprint())).isSameAs(grandchild);
    assertThat(child.findChild("herman")).isNull();
  }

  @Test public void requireChildWithOneModule() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new CharlieBlueprint());

    ObjectGraph objectGraph = getObjectGraph(child);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
  }

  @Test public void requireChildWithMoreModules() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope activityScope = ObjectGraphService.requireActivityScope(root, new BakerBlueprint());
    MortarScope child = requireChild(activityScope, new MoreModules());

    ObjectGraph objectGraph = getObjectGraph(child);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
  }

  @Test public void requireChildWithNoModules() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope child = requireChild(root, new NoModules());

    ObjectGraph objectGraph = getObjectGraph(child);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
  }

  @Test public void handlesRecursiveDestroy() {
    final AtomicInteger i = new AtomicInteger(0);

    final MortarScope scope = createRootScope(create(new Able()));
    scope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        i.incrementAndGet();
        scope.destroy();
      }
    });
    scope.destroy();
    assertThat(i.get()).isEqualTo(1);
  }

  @Test public void inject() {
    final MortarScope root = createRootScope(create(new Able()));
    when(context.getSystemService(any(String.class))).then(new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        return root.getService((String) invocation.getArguments()[0]);
      }
    });
    HasApple apple = new HasApple();
    ObjectGraphService.inject(context, apple);
    assertThat(apple.string).isEqualTo(Apple.class.getName());
  }

  @Test public void getScope() {
    MortarScope root = createRootScope(create(new Able()));
    when(context.getSystemService(MortarScope.SERVICE_NAME)).thenReturn(root);
    assertThat(MortarScope.getScope(context)).isSameAs(root);
  }

  @Test public void canGetNameFromDestroyed() {
    MortarScope scope = createRootScope(create(new Able()));
    String name = scope.getName();
    assertThat(name).isNotNull();
    scope.destroy();
    assertThat(scope.getName()).isEqualTo(name);
  }

  @Test public void cannotGetObjectGraphFromDestroyed() {
    MortarScope scope = createRootScope(create(new Able()));
    scope.destroy();

    IllegalStateException caught = null;
    try {
      getObjectGraph(scope);
    } catch (IllegalStateException e) {
      caught = e;
    }
    assertThat(caught).isNotNull();
  }

  @Test(expected = IllegalStateException.class) public void cannotRegisterOnDestroyed() {
    MortarScope scope = createRootScope(create(new Able()));
    scope.destroy();
    scope.register(scoped);
  }

  @Test(expected = IllegalStateException.class) public void cannotFindChildFromDestroyed() {
    MortarScope scope = createRootScope(create(new Able()));
    scope.destroy();
    scope.findChild("foo");
  }

  @Test(expected = IllegalStateException.class) public void cannotRequireChildFromDestroyed() {
    MortarScope scope = createRootScope(create(new Able()));
    scope.destroy();
    requireChild(scope, new AbleBlueprint());
  }

  @Test public void destroyIsIdempotent() {
    MortarScope root = createRootScope(create(new Able()));
    MortarScope child = requireChild(root, new NoModules());

    final AtomicInteger destroys = new AtomicInteger(0);
    child.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        destroys.addAndGet(1);
      }
    });

    child.destroy();
    assertThat(destroys.get()).isEqualTo(1);

    child.destroy();
    assertThat(destroys.get()).isEqualTo(1);
  }

  @Test public void rootDestroyIsIdempotent() {
    MortarScope scope = createRootScope(create(new Able()));

    final AtomicInteger destroys = new AtomicInteger(0);
    scope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        destroys.addAndGet(1);
      }
    });

    scope.destroy();
    assertThat(destroys.get()).isEqualTo(1);

    scope.destroy();
    assertThat(destroys.get()).isEqualTo(1);
  }

  @Test public void isDestroyedStartsFalse() {
    MortarScope root = createRootScope(create(new Able()));
    assertThat(root.isDestroyed()).isFalse();
  }

  @Test public void isDestroyedGetsSet() {
    MortarScope root = createRootScope(create(new Able()));
    root.destroy();
    assertThat(root.isDestroyed()).isTrue();
  }

  private static MortarScope createRootScope(ObjectGraph objectGraph) {
    return MortarScope.buildRootScope()
        .withService(ObjectGraphService.SERVICE_NAME, objectGraph)
        .build("Root");
  }
}
