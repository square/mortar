package mortar;

public interface MortarServiceProvider {
  String getName();
  Object getService(MortarScope scope);
}
