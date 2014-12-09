package mortar.dagger2support;

import android.content.Context;
import java.lang.reflect.Method;
import mortar.Mortar;

public class Dagger2 {

  public static <T> T get(Context context) {
    return Mortar.getScope(context).getObjectGraph();
  }

  /**
   * Magic method that creates a component with its dependencies set, by reflection. Relies on
   * Dagger2 naming conventions.
   */
  public static <T> T buildComponent(Class<T> componentClass, Object... dependencies) {
    String fqn = componentClass.getName();

    String packageName = componentClass.getPackage().getName();
    // Accounts for inner classes, ie MyApplication$Component
    String simpleName = fqn.substring(packageName.length() + 1);

    try {
      Class<?> generatedClass = Class.forName(packageName + ".Dagger_" + simpleName);
      Object builder = generatedClass.getMethod("builder").invoke(null);

      for (Method method : builder.getClass().getMethods()) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1) {
          Class<?> dependencyClass = params[0];
          for (Object dependency : dependencies) {
            if (dependencyClass.isAssignableFrom(dependency.getClass())) {
              method.invoke(builder, dependency);
              break;
            }
          }
        }
      }
      //noinspection unchecked
      return (T) builder.getClass().getMethod("build").invoke(builder);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
