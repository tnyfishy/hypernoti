package dev.hypernoti;
public final class Readiness {
 private Readiness() {}
 public static boolean googleAvailable(boolean installed, boolean enabled) { return installed && enabled; }
 public static int completed(boolean[] items) { int count = 0; for (boolean item : items) if (item) count++; return count; }
}
