package mortar.dagger1support;

import dagger.ObjectGraph;
import java.util.Collection;
import mortar.MortarScope;

/**
 * Defines a scope to be built via {@link Dagger1#requireChild(MortarScope, Blueprint)} or
 * {@link Dagger1#requireActivityScope(MortarScope, Blueprint)}.
 */
public interface Blueprint {
  /**
   * Returns the name of the new scope. This can be used later to {@link
   * MortarScope#findChild(String) find} it in its parent. If {@link
   * Dagger1#requireChild(MortarScope, Blueprint)} is called again with a {@link
   * Blueprint} of the same name, the original instance will be returned unless it has been
   * {@link MortarScope#destroyChild(MortarScope)}  destroyed}.
   */
  String getMortarScopeName();

  /**
   * Returns the {@literal @}{@link dagger.Module Module} that will define the scope
   * of the new graph by being added to that of its parent. If the returned value
   * is an instance of {@link Collection} its contents will be used as modules.
   * Returns null if this scope needs no modules.
   *
   * @see ObjectGraph#plus(Object...)
   */
  Object getDaggerModule();
}
