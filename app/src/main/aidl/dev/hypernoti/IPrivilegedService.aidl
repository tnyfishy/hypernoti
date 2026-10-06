package dev.hypernoti;
interface IPrivilegedService {
 String apply(String packageName, boolean enable) = 0;
 void destroy() = 16777114;
}
