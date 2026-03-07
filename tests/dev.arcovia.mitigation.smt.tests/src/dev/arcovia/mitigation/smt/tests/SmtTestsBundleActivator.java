package dev.arcovia.mitigation.smt.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import dev.arcovia.mitigation.smt.utils.Z3NativeLoader;

/**
 * Ensures Z3 natives are loaded before SMT tests touch the Java bindings directly.
 */
public class SmtTestsBundleActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        Z3NativeLoader.ensureLoaded();
    }

    @Override
    public void stop(BundleContext context) {
        // Nothing to clean up. Native libraries remain loaded for the JVM lifetime.
    }
}
