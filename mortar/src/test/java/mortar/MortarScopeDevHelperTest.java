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

import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;

import static mortar.Mortar.createRootScope;
import static mortar.MortarScopeDevHelper.scopeHierarchyToString;
import static org.fest.assertions.api.Assertions.assertThat;

public class MortarScopeDevHelperTest {
  private static final char BLANK = '\u00a0';

  class CustomScope implements Blueprint {
    private final String name;
    private Object module;

    CustomScope(String name) {
      this(name, null);
    }

    CustomScope(String name, Object module) {
      this.name = name;
      this.module = module;
    }

    @Override public String getMortarScopeName() {
      return name;
    }

    @Override public Object getDaggerModule() {
      return module;
    }
  }

  static class EntryPoint1 {
  }

  static class EntryPoint2 {
  }

  static class EntryPoint3 {
  }

  @Module(injects = EntryPoint1.class) class SimpleModule {
  }

  @Module(injects = { EntryPoint2.class, EntryPoint3.class }) class TwoInjectsModule {
  }

  @Test public void nestedScopeHierarchyToString() {
    MortarScope root = createRootScope(false, emptyObjectGraph());
    root.requireChild(new CustomScope("Cadet"));

    MortarScope colonel = root.requireChild(new CustomScope("Colonel"));
    colonel.requireChild(new CustomScope("ElderColonel"));
    colonel.requireChild(new CustomScope("ZeElderColonel"));

    MortarScope elder = root.requireChild(new CustomScope("Elder"));
    elder.requireChild(new CustomScope("ElderCadet"));
    elder.requireChild(new CustomScope("ZeElderCadet"));
    elder.requireChild(new CustomScope("ElderElder"));
    elder.requireChild(new CustomScope("AnElderCadet"));


    String hierarchy = scopeHierarchyToString(root);
    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "+-SCOPE Cadet\n" //
        + BLANK + "+-SCOPE Colonel\n" //
        + BLANK + "| +-SCOPE ElderColonel\n" //
        + BLANK + "| `-SCOPE ZeElderColonel\n" //
        + BLANK + "`-SCOPE Elder\n" //
        + BLANK + "  +-SCOPE AnElderCadet\n" //
        + BLANK + "  +-SCOPE ElderCadet\n" //
        + BLANK + "  +-SCOPE ElderElder\n" //
        + BLANK + "  `-SCOPE ZeElderCadet\n" //
    );
  }

  @Test public void startsFromRootScope() {
    MortarScope root = createRootScope(false, emptyObjectGraph());
    MortarScope child = root.requireChild(new CustomScope("Child"));

    String hierarchy = scopeHierarchyToString(child);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-SCOPE Child\n" //
    );
  }

  @Test public void noSpaceAtLineBeginnings() {
    MortarScope root = createRootScope(false, emptyObjectGraph());
    MortarScope child = root.requireChild(new CustomScope("Child"));
    child.requireChild(new CustomScope("Grand Child"));

    String hierarchy = scopeHierarchyToString(root);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-SCOPE Child\n" //
        + BLANK + "  `-SCOPE Grand Child\n" //
    );
  }

  private ObjectGraph emptyObjectGraph() {
    return ObjectGraph.create();
  }

  @Test public void simpleModule() {
    MortarScope root = createRootScope(false, ObjectGraph.create(new SimpleModule()));
    String hierarchy = scopeHierarchyToString(root);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-MODULE " + SimpleModule.class.getName() + "\n" //
        + BLANK + "  `-INJECT " + EntryPoint1.class.getName() + "\n" //
    );
  }

  @Test public void twoInjects() {
    MortarScope root = createRootScope(false, ObjectGraph.create(new TwoInjectsModule()));
    String hierarchy = scopeHierarchyToString(root);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-MODULE " + TwoInjectsModule.class.getName() + "\n" //
        + BLANK + "  +-INJECT " + EntryPoint2.class.getName() + "\n" //
        + BLANK + "  `-INJECT " + EntryPoint3.class.getName() + "\n" //
    );
  }

  @Test public void moduleInChildScope() {
    MortarScope root = createRootScope(false, ObjectGraph.create(new SimpleModule()));
    root.requireChild(new CustomScope("Child", new TwoInjectsModule()));

    String hierarchy = scopeHierarchyToString(root);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "+-MODULE " + SimpleModule.class.getName() + "\n" //
        + BLANK + "| `-INJECT " + EntryPoint1.class.getName() + "\n" //
        + BLANK + "`-SCOPE Child\n" //
        + BLANK + "  `-MODULE " + TwoInjectsModule.class.getName() + "\n" //
        + BLANK + "    +-INJECT " + EntryPoint2.class.getName() + "\n" //
        + BLANK + "    `-INJECT " + EntryPoint3.class.getName() + "\n" //
    );
  }
}
