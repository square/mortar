package mortar;

import dagger.ObjectGraph;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MortarScopeDevHelper {

  /**
   * Format the scope hierarchy as a multi line string containing the scope names.
   * Can be given any scope in the hierarchy, will always print the whole scope hierarchy.
   * Also prints the Dagger modules containing entry points (injects). We've only tested this with
   * Dagger 1.1.0, please report any bug you may find.
   */
  public static String scopeHierarchyToString(MortarScope mortarScope) {
    StringBuilder result = new StringBuilder("Mortar Hierarchy:\n");
    MortarScope MortarScope = getMortarScope(mortarScope);
    Node rootNode = new MortarScopeNode(MortarScope);
    nodeHierarchyToString(result, 0, 0, rootNode);
    return result.toString();
  }

  interface Node {
    String getName();

    List<Node> getChildNodes();
  }

  static class MortarScopeNode implements Node {

    private final MortarScope mortarScope;

    MortarScopeNode(MortarScope mortarScope) {
      this.mortarScope = mortarScope;
    }

    @Override public String getName() {
      return "SCOPE " + mortarScope.getName();
    }

    @Override public List<Node> getChildNodes() {
      List<Node> childNodes = new ArrayList<Node>();
      ModuleNode.addModuleChildren(mortarScope, childNodes);
      addScopeChildren(childNodes);
      return childNodes;
    }

    private void addScopeChildren(List<Node> childNodes) {
      if (!(mortarScope instanceof RealScope)) {
        return;
      }
      RealScope realScope = (RealScope) mortarScope;
      for (MortarScope childScope : realScope.children.values()) {
        childNodes.add(new MortarScopeNode(childScope));
      }
    }
  }

  static class ModuleNode implements Node {

    private static Class<?> OBJECT_GRAPH_CLASS;
    private static Field INJECTABLE_TYPES_FIELD;
    private static boolean COULD_NOT_LOAD;

    static {
      try {
        OBJECT_GRAPH_CLASS = Class.forName("dagger.ObjectGraph$DaggerObjectGraph");
        INJECTABLE_TYPES_FIELD = OBJECT_GRAPH_CLASS.getDeclaredField("injectableTypes");
        INJECTABLE_TYPES_FIELD.setAccessible(true);
      } catch (Exception e) {
        COULD_NOT_LOAD = true;
      }
    }

    static void addModuleChildren(MortarScope mortarScope, List<Node> childNodes) {
      if (COULD_NOT_LOAD) {
        childNodes.add(new Node() {
          @Override public String getName() {
            return "ERROR Could not access Dagger fields";
          }

          @Override public List<Node> getChildNodes() {
            return Collections.emptyList();
          }
        });
        return;
      }

      ObjectGraph objectGraph = mortarScope.getObjectGraph();

      if (!OBJECT_GRAPH_CLASS.isInstance(objectGraph)) {
        throw new IllegalArgumentException(
            objectGraph + " is not an instance of " + OBJECT_GRAPH_CLASS);
      }

      Map<String, Class<?>> injectableTypes;

      try {
        //noinspection unchecked
        injectableTypes = (Map<String, Class<?>>) INJECTABLE_TYPES_FIELD.get(objectGraph);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      // Mapping Map<Inject, Module> to Map<Module, List<Inject>>
      Map<Class<?>, List<Node>> injectsByModule = new LinkedHashMap<Class<?>, List<Node>>();
      for (Map.Entry<String, Class<?>> injectableType : injectableTypes.entrySet()) {
        Class<?> moduleClass = injectableType.getValue();
        List<Node> moduleInjects = injectsByModule.get(moduleClass);
        if (moduleInjects == null) {
          moduleInjects = new ArrayList<Node>();
          injectsByModule.put(moduleClass, moduleInjects);
        }
        moduleInjects.add(new InjectNode(injectableType.getKey()));
      }

      Set<Map.Entry<Class<?>, List<Node>>> injectsByModuleSet = injectsByModule.entrySet();
      for (Map.Entry<Class<?>, List<Node>> moduleAndInjects : injectsByModuleSet) {
        childNodes.add(new ModuleNode(moduleAndInjects.getKey(), moduleAndInjects.getValue()));
      }
    }

    private final Class<?> moduleClass;
    private final List<Node> injects;

    ModuleNode(Class<?> moduleClass, List<Node> injects) {
      this.moduleClass = moduleClass;
      this.injects = injects;
    }

    @Override public String getName() {
      return "MODULE " + moduleClass.getName();
    }

    @Override public List<Node> getChildNodes() {
      return injects;
    }
  }

  static class InjectNode implements Node {
    private static final int MEMBER_PREFIX = "members/".length();

    private final String name;

    InjectNode(String moduleMember) {
      // Inject keys name starts with "members/" which we don't care about.
      this.name = "INJECT " + moduleMember.substring(MEMBER_PREFIX);
    }

    @Override public String getName() {
      return name;
    }

    @Override public List<Node> getChildNodes() {
      return Collections.emptyList();
    }
  }

  private static MortarScope getMortarScope(MortarScope mortarScope) {
    if (!(mortarScope instanceof RealScope)) {
      return mortarScope;
    }
    RealScope scope = (RealScope) mortarScope;
    while (scope.getParent() != null) {
      scope = scope.getParent();
    }
    return scope;
  }

  private static void nodeHierarchyToString(StringBuilder result, int depth, long lastChildMask,
      Node node) {
    appendLinePrefix(result, depth, lastChildMask);
    result.append(node.getName()).append('\n');

    List<Node> childNodes = node.getChildNodes();
    Collections.sort(childNodes, new NodeSorter());

    int lastIndex = childNodes.size() - 1;
    int index = 0;
    for (Node childNode : childNodes) {
      if (index == lastIndex) {
        lastChildMask = lastChildMask | (1 << depth);
      }
      nodeHierarchyToString(result, depth + 1, lastChildMask, childNode);
      index++;
    }
  }

  private static void appendLinePrefix(StringBuilder result, int depth, long lastChildMask) {
    int lastDepth = depth - 1;
    // Add a non-breaking space at the beginning of the line because Logcat eats normal spaces.
    result.append('\u00a0');
    for (int parentDepth = 0; parentDepth <= lastDepth; parentDepth++) {
      if (parentDepth > 0) {
        result.append(' ');
      }
      boolean lastChild = (lastChildMask & (1 << parentDepth)) != 0;
      if (lastChild) {
        if (parentDepth == lastDepth) {
          result.append('`');
        } else {
          result.append(' ');
        }
      } else {
        if (parentDepth == lastDepth) {
          result.append('+');
        } else {
          result.append('|');
        }
      }
    }
    if (depth > 0) {
      result.append("-");
    }
  }

  private MortarScopeDevHelper() {
    throw new UnsupportedOperationException("This is a helper class");
  }

  private static class NodeSorter implements Comparator<Node> {
    @Override public int compare(Node lhs, Node rhs) {
      return lhs.getName().compareTo(rhs.getName());
    }
  }
}
