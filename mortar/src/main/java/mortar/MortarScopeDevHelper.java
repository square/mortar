package mortar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MortarScopeDevHelper {

  /**
   * Format the scope hierarchy as a multi line string containing the scope names.
   * Can be given any scope in the hierarchy, will always print the whole scope hierarchy.
   * Also prints the Dagger modules containing entry points (injects). We've only tested this with
   * Dagger 1.1.0, please report any bug you may find.
   */
  public static String scopeHierarchyToString(MortarScope mortarScope) {
    StringBuilder result = new StringBuilder("Mortar Hierarchy:\n");
    MortarScope rootScope = getRootScope(mortarScope);
    Node rootNode = new MortarScopeNode(rootScope);
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
      List<Node> childNodes = new ArrayList<>();
      addScopeChildren(childNodes);
      return childNodes;
    }

    private void addScopeChildren(List<Node> childNodes) {
      for (MortarScope childScope : mortarScope.children.values()) {
        childNodes.add(new MortarScopeNode(childScope));
      }
    }
  }


  private static MortarScope getRootScope(MortarScope scope) {
    while (scope.parent != null) {
      scope = scope.parent;
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
