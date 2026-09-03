package com.majortom.algorithms.core.registry;

import java.util.Map;

/** Startup-time discovery hook for modules that can derive registrations from their own classpath. */
public interface ModuleDiscovery {
    Map<String, Class<?>> discover(ClassLoader classLoader);
}
