package dev.arcovia.mitigation.smt;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import dev.arcovia.mitigation.smt.util.Z3NativeLoader;

/**
 * Loads the bundled Z3 natives when the SMT bundle starts in OSGi.
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        Z3NativeLoader.ensureLoaded();
    }

    @Override
    public void stop(BundleContext context) {
        // Nothing to clean up. Native libraries remain loaded for the JVM lifetime.
    }
}
