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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static dagger.ObjectGraph.create;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static mortar.Mortar.MORTAR_SCOPE_SERVICE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("InnerClassMayBeStatic")
public class MortarScopeTest {

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

  @Module(injects = HasApple.class) class Able implements Blueprint {
    @Provides @Apple String provideApple() {
      return Apple.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideApple();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasBagel.class) class Baker implements Blueprint {
    @Provides @Bagel String provideBagel() {
      return Bagel.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideBagel();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasCarrot.class) class Charlie implements Blueprint {
    @Provides @Carrot String provideCharlie() {
      return Carrot.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideCharlie();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasDogfood.class) class Delta implements Blueprint {
    @Provides @Dogfood String provideDogfood() {
      return Dogfood.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideDogfood();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasEggplant.class) class Echo implements Blueprint {
    @Provides @Eggplant String provideEggplant() {
      return Eggplant.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideEggplant();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = DoImpossible.class, complete = false, library = true) class Impossible
      implements Blueprint {
    @Override public String getMortarScopeName() {
      return "Impossible";
    }

    @Override public Object getDaggerModule() {
      return this;
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

  static class DoImpossible {
    @SuppressWarnings("UnusedDeclaration") @Inject float floot;
  }

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void rootDoesNotValidate() {
    Mortar.createRootScope(false, create(new Impossible()));
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void rootDoesValidate() {
    Mortar.createRootScope(true, create(new Impossible()));
  }

  @Test
  public void rootScopeHasName() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    assertThat(scope.getName()).isSameAs(MortarScope.ROOT_NAME);
  }

  @Test
  public void createRootScopeUsesModules() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able(), new Baker()));
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

  @Test
  public void destroyRoot() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.register(scoped);
    scope.destroy();
    verify(scoped).onDestroy();
  }

  @Test
  public void activityDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    Mortar.requireActivityScope(root, new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void activityDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, create(new Able()));
    Mortar.requireActivityScope(root, new Impossible());
  }

  @Test
  public void activityScopeName() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());

    String bagel = Bagel.class.getName();
    assertThat(activityScope.getName()).isEqualTo(bagel);
    assertThat(root.findChild(bagel)).isSameAs(activityScope);
    assertThat(root.requireChild(new Baker())).isSameAs(activityScope);
    assertThat(Mortar.requireActivityScope(root, new Baker())).isSameAs(activityScope);
    assertThat(root.findChild("herman")).isNull();
  }

  @Test
  public void getActivityScopeWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
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

  @Test
  public void getActivityScopeWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false,create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new MoreModules());
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

  @Test
  public void destroyActivityScopeDirect() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    Baker blueprint = new Baker();
    MortarScope activityScope = Mortar.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    activityScope.destroy();
    verify(scoped).onDestroy();
    assertThat(root.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void destroyActivityScopeRecursive() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    Baker blueprint = new Baker();
    MortarScope activityScope = Mortar.requireActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    root.destroy();
    verify(scoped).onDestroy();
    try {
      activityScope.getObjectGraph();
      fail("Expected IllegalStateException from destroyed child");
    } catch (IllegalStateException e) {
      // pass;
    }
  }

  @Test
  public void activityChildDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    activityScope.requireChild(new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void activityChildDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    activityScope.requireChild(new Impossible());
  }

  @Test
  public void activityChildScopeName() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(new Charlie());

    String carrot = Carrot.class.getName();
    assertThat(child.getName()).isEqualTo(carrot);
    assertThat(activityScope.findChild(carrot)).isSameAs(child);
    assertThat(activityScope.requireChild(new Charlie())).isSameAs(child);
    assertThat(activityScope.findChild("herman")).isNull();
  }

  @Test
  public void requireGrandchildWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new Delta());
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

  @Test
  public void requireGrandchildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new MoreModules());

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

  @Test
  public void requireGrandchildWithNoModules() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new NoModules());

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

  @Test
  public void destroyActivityChildScopeDirect() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    Charlie blueprint = new Charlie();
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    child.destroy();
    verify(scoped).onDestroy();
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void destroyActivityChildScopeRecursive() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    Charlie blueprint = new Charlie();
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    root.destroy();
    verify(scoped).onDestroy();
    try {
      child.getObjectGraph();
      fail("Expected IllegalStateException from destroyed child");
    } catch (IllegalStateException e) {
      // pass;
    }
  }

  @Test
  public void activityGrandchildDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false,create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    activityScope.requireChild(new Delta()).requireChild(new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void activityGrandchildDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, create(new Able()));
    MortarActivityScope activityScope = Mortar.requireActivityScope(root, new Baker());
    activityScope.requireChild(new Delta()).requireChild(new Impossible());
  }

  @Test
  public void activityGrandchildScopeName() {
    MortarScope root = Mortar.createRootScope(false,create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new Delta());

    String dogfood = Dogfood.class.getName();
    assertThat(grandchild.getName()).isEqualTo(dogfood);
    assertThat(child.findChild(dogfood)).isSameAs(grandchild);
    assertThat(child.requireChild(new Delta())).isSameAs(grandchild);
    assertThat(child.findChild("herman")).isNull();
  }

  @Test
  public void requireChildWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new Charlie());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
  }

  @Test
  public void requireChildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = Mortar.requireActivityScope(root, new Baker()).requireChild(new MoreModules());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
  }

  @Test
  public void requireChildWithNoModules() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    MortarScope child = root.requireChild(new NoModules());

    ObjectGraph objectGraph = child.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
  }

  @Test
  public void handlesRecursiveDestroy() {
    final AtomicInteger i = new AtomicInteger(0);

    final MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.register(new Scoped() {
      @Override public void onDestroy() {
        i.incrementAndGet();
        scope.destroy();
      }
    });
    scope.destroy();
    assertThat(i.get()).isEqualTo(1);
  }

  @Test
  public void inject() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    when(context.getSystemService(MORTAR_SCOPE_SERVICE)).thenReturn(root);
    HasApple apple = new HasApple();
    Mortar.inject(context, apple);
    assertThat(apple.string).isEqualTo(Apple.class.getName());
  }

  @Test
  public void getScope() {
    MortarScope root = Mortar.createRootScope(false, create(new Able()));
    when(context.getSystemService(MORTAR_SCOPE_SERVICE)).thenReturn(root);
    assertThat(Mortar.getScope(context)).isSameAs(root);
  }

  @Test
  public void canGetNameFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    assertThat(scope.getName()).isEqualTo(MortarScope.ROOT_NAME);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotGetObjectGraphFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    scope.getObjectGraph();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotRegisterOnDestroyed() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    scope.register(scoped);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotFindChildFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    scope.findChild("foo");
  }

  @Test(expected = IllegalStateException.class)
  public void cannotRequireChildFromDestroyed() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    scope.requireChild(new Able());
  }

  @Test
  public void destroyIsIdempotent() {
    MortarScope scope = Mortar.createRootScope(false, create(new Able()));
    scope.destroy();
    scope.destroy();
    // Ta da.
  }
}
