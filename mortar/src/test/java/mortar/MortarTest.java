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

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MortarTest {
  @Mock Context rawContext;
  @Mock(extraInterfaces = MortarContext.class) Context goodContext;
  @Mock MortarScope scope;

  @Before
  public void setUp() {
    initMocks(this);
    when(((MortarContext) goodContext).getMortarScope()).thenReturn(scope);
  }

  @Test
  public void testGood() {
    assertThat(Mortar.getScope(goodContext)).isSameAs(scope);
  }

  @Test
  public void testBad() {
    try {
      Mortar.getScope(rawContext);
      fail("Expected exception");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).startsWith("Mortar requires android.content.Context");
      assertThat(e.getMessage()).endsWith("(and all Contexts) to implement mortar.MortarContext");
    }
  }
}
