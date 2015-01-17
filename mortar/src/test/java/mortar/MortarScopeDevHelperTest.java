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

import org.junit.Test;

import static mortar.MortarScopeDevHelper.scopeHierarchyToString;
import static org.fest.assertions.api.Assertions.assertThat;

public class MortarScopeDevHelperTest {
  private static final char BLANK = '\u00a0';

  @Test public void nestedScopeHierarchyToString() {
    MortarScope root = MortarScope.Builder.ofRoot().build();
    root.buildChild("Cadet").build();

    MortarScope colonel = root.buildChild("Colonel").build();
    colonel.buildChild("ElderColonel").build();
    colonel.buildChild("ZeElderColonel").build();

    MortarScope elder = root.buildChild("Elder").build();
    elder.buildChild("ElderCadet");
    elder.buildChild("ZeElderCadet");
    elder.buildChild("ElderElder");
    elder.buildChild("AnElderCadet");

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

  @Test public void startsFromMortarScope() {
    MortarScope root = MortarScope.Builder.ofRoot().build();
    MortarScope child = root.buildChild("Child").build();

    String hierarchy = scopeHierarchyToString(child);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-SCOPE Child\n" //
    );
  }

  @Test public void noSpaceAtLineBeginnings() {
    MortarScope root = MortarScope.Builder.ofRoot().build();
    MortarScope child = root.buildChild("Child").build();
    child.buildChild("Grand Child").build();

    String hierarchy = scopeHierarchyToString(root);

    assertThat(hierarchy).isEqualTo("" //
        + "Mortar Hierarchy:\n" //
        + BLANK + "SCOPE Root\n" //
        + BLANK + "`-SCOPE Child\n" //
        + BLANK + "  `-SCOPE Grand Child\n" //
    );
  }
}
