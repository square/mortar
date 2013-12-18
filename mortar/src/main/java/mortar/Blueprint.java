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

/**
 * Defines a scope to be built via {@link MortarScope#requireChild(Blueprint)} or
 * {@link Mortar#requireActivityScope(MortarScope, Blueprint)}.
 */
public interface Blueprint {
  /**
   * Returns the name of the new scope. This can be used later to {@link
   * MortarScope#findChild(String) find} it in its parent. If {@link
   * MortarScope#requireChild(Blueprint)} is called again with a {@link Blueprint}
   * of the same name, the original instance will be returned unless it has been
   * {@link MortarScope#destroy() destroyed}.
   */
  String getMortarScopeName();

  /**
   * Returns the {@literal @}{@link dagger.Module Module} that will define the scope
   * of the new graph by being added to that of its parent. If the returned value
   * is an instance of {@link java.util.Collection} its contents will be used as modules.
   * Returns null if this scope needs no modules.
   *
   * @see dagger.ObjectGraph#plus(Object...)
   */
  Object getDaggerModule();
}
