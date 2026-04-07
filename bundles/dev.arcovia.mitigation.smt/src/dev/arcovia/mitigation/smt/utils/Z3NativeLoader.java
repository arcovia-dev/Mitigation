package dev.arcovia.mitigation.smt.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Loads the bundled Z3 native libraries before the Java bindings initialize.
 */
public final class Z3NativeLoader {

    private static final String Z3_SKIP_LIBRARY_LOAD = "z3.skipLibraryLoad";
    private static boolean loaded;

    private Z3NativeLoader() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        System.setProperty(Z3_SKIP_LIBRARY_LOAD, Boolean.TRUE.toString());

        String os = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "")
                .toLowerCase(Locale.ROOT);
        NativePlatform platform = NativePlatform.detect(os, arch);

        try {
            Path nativeDir = Files.createTempDirectory("z3-natives-");
            nativeDir.toFile()
                    .deleteOnExit();

            Path z3 = extract(nativeDir, platform.z3LibraryPath());
            Path z3java = extract(nativeDir, platform.z3JavaLibraryPath());

            System.load(z3.toAbsolutePath()
                    .toString());
            System.load(z3java.toAbsolutePath()
                    .toString());
            loaded = true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load bundled Z3 natives", e);
        }
    }

    private static Path extract(Path nativeDir, String resourcePath) throws IOException {
        try (InputStream input = openBundledResource(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing bundled native resource: " + resourcePath);
            }

            Path extracted = nativeDir.resolve(Path.of(resourcePath)
                    .getFileName()
                    .toString());
            Files.copy(input, extracted, StandardCopyOption.REPLACE_EXISTING);
            extracted.toFile()
                    .deleteOnExit();
            return extracted;
        }
    }

    private static InputStream openBundledResource(String resourcePath) throws IOException {
        InputStream bundleStream = openBundleEntry(resourcePath);
        if (bundleStream != null) {
            return bundleStream;
        }

        InputStream classpathStream = Z3NativeLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (classpathStream != null) {
            return classpathStream;
        }

        Path filesystemPath = resolveFilesystemResource(resourcePath);
        if (filesystemPath != null) {
            return Files.newInputStream(filesystemPath);
        }

        return null;
    }

    private static InputStream openBundleEntry(String resourcePath) throws IOException {
        Object bundle = getOsgiBundle();
        if (bundle == null) {
            return null;
        }

        URL entry = getBundleEntry(bundle, resourcePath);
        if (entry == null) {
            return null;
        }

        return entry.openStream();
    }

    private static Object getOsgiBundle() {
        try {
            Class<?> frameworkUtil = Class.forName("org.osgi.framework.FrameworkUtil");
            Method getBundle = frameworkUtil.getMethod("getBundle", Class.class);
            return getBundle.invoke(null, Z3NativeLoader.class);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static URL getBundleEntry(Object bundle, String resourcePath) {
        try {
            Method getEntry = bundle.getClass()
                    .getMethod("getEntry", String.class);
            return (URL) getEntry.invoke(bundle, "/" + resourcePath);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static Path resolveFilesystemResource(String resourcePath) {
        try {
            URL location = Z3NativeLoader.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            Path codeSource = Path.of(location.toURI());

            if (Files.isDirectory(codeSource)) {
                Path direct = codeSource.resolve(resourcePath);
                if (Files.exists(direct)) {
                    return direct;
                }

                Path sibling = codeSource.getParent()
                        .resolve(resourcePath);
                if (Files.exists(sibling)) {
                    return sibling;
                }
            }
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }

        return null;
    }

    private record NativePlatform(String libraryDir, String z3LibraryName, String z3JavaLibraryName) {

        private static NativePlatform detect(String os, String arch) {
            String normalizedArch = normalizeArch(arch);

            if (os.contains("linux")) {
                return switch (normalizedArch) {
                    case "x64" -> new NativePlatform("lib/x64/glibc", "libz3.so", "libz3java.so");
                    case "arm64" -> new NativePlatform("lib/arm64/glibc", "libz3.so", "libz3java.so");
                    default -> unsupported(os, arch);
                };
            }

            if (os.contains("mac") || os.contains("darwin")) {
                return switch (normalizedArch) {
                    case "x64" -> new NativePlatform("lib/x64/osx", "libz3.dylib", "libz3java.dylib");
                    case "arm64" -> new NativePlatform("lib/arm64/osx", "libz3.dylib", "libz3java.dylib");
                    default -> unsupported(os, arch);
                };
            }

            if (os.contains("win")) {
                return switch (normalizedArch) {
                    case "x86" -> new NativePlatform("lib/x86/win", "libz3.dll", "libz3java.dll");
                    case "x64" -> new NativePlatform("lib/x64/win", "libz3.dll", "libz3java.dll");
                    case "arm64" -> new NativePlatform("lib/arm64/win", "libz3.dll", "z3java.dll");
                    default -> unsupported(os, arch);
                };
            }

            throw new IllegalStateException("Unsupported platform for bundled Z3 natives: " + os + " / " + arch);
        }

        private static String normalizeArch(String arch) {
            return switch (arch) {
                case "x86", "i386", "i486", "i586", "i686" -> "x86";
                case "x86_64", "amd64", "x64" -> "x64";
                case "aarch64", "arm64" -> "arm64";
                default -> arch;
            };
        }

        private static NativePlatform unsupported(String os, String arch) {
            throw new IllegalStateException("Unsupported platform for bundled Z3 natives: " + os + " / " + arch);
        }

        private String z3LibraryPath() {
            return libraryDir + "/" + z3LibraryName;
        }

        private String z3JavaLibraryPath() {
            return libraryDir + "/" + z3JavaLibraryName;
        }
    }
}
