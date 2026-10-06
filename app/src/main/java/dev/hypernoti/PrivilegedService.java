package dev.hypernoti;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
public class PrivilegedService extends IPrivilegedService.Stub {
 public PrivilegedService() {}
 @Override public String apply(String packageName, boolean enable) {
  try {
   StringBuilder result = new StringBuilder();
   for (String[] command : CommandPolicy.commands(packageName, enable)) {
    result.append(run(command)).append("\n");
   }
   return result.toString();
  } catch (Exception e) { return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
 }
 public static String run(String[] command) throws Exception {
  Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
  ByteArrayOutputStream output = new ByteArrayOutputStream();
  Thread reader = new Thread(() -> {
   try (InputStream stream = process.getInputStream()) {
    byte[] buffer = new byte[1024]; int count;
    while ((count = stream.read(buffer)) != -1) {
     synchronized (output) { if (output.size() < 16384) output.write(buffer, 0, Math.min(count, 16384 - output.size())); }
    }
   } catch (Exception ignored) {}
  });
  reader.start();
  if (!process.waitFor(30, TimeUnit.SECONDS)) {
   process.destroyForcibly(); reader.join(1000); return "ERROR: command timed out";
  }
  reader.join(1000);
  synchronized (output) { return "exit=" + process.exitValue() + " " + output.toString("UTF-8"); }
 }
 @Override public void destroy() { System.exit(0); }
}
