package me.z7087.vectorapiloader;

import me.z7087.ilg.ImplLookupGetter;

import java.lang.ModuleLayer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Set;
import java.util.function.Function;

public final class VectorApiLoader {

    private VectorApiLoader() {}

    private static final boolean JAVA9;

    static {
        boolean j9;
        try {
            Class.forName("java.lang.Module", false, null);
            j9 = true;
        } catch (ClassNotFoundException e) {
            j9 = false;
        }
        JAVA9 = j9;
    }

    private static final MethodHandle TRUE_MH = MethodHandles.constant(boolean.class, true);
    private static final MethodHandle FALSE_MH = MethodHandles.constant(boolean.class, false);
    private static final MethodHandle UNSET_MH = MethodHandles.throwException(boolean.class, IllegalStateException.class).bindTo(new IllegalStateException("uninitialized"));

    private static final VolatileCallSite VECTOR_LOADED = new VolatileCallSite(FALSE_MH);
    private static final VolatileCallSite VECTOR_SUPPORTED = new VolatileCallSite(UNSET_MH);

    public static final String VECTOR_MODULE_NAME = "jdk.incubator.vector";

    public static boolean isLoaded() {
        if (!JAVA9)
            return false;
        if (VECTOR_LOADED.getTarget() == TRUE_MH)
            return true;
        try {
            Class.forName("jdk.incubator.vector.Vector", false, null);
        } catch (ClassNotFoundException e) {
            return false;
        }
        VECTOR_LOADED.setTarget(TRUE_MH);
        return true;
    }

    @SuppressWarnings("Since15")
    public static boolean isSupported() {
        if (!JAVA9)
            return false;
        {
            final MethodHandle vectorSupportedSupplier = VECTOR_SUPPORTED.getTarget();
            if (vectorSupportedSupplier == TRUE_MH)
                return true;
            if (vectorSupportedSupplier == FALSE_MH)
                return false;
        }
        final boolean result = ModuleFinder.ofSystem().find(VECTOR_MODULE_NAME).isPresent();
        VECTOR_SUPPORTED.setTarget(result ? TRUE_MH : FALSE_MH);
        return result;
    }

    @SuppressWarnings("Since15")
    public static boolean tryLoad() {
        if (!JAVA9)
            return false;
        if (!isSupported())
            return false;
        if (isLoaded())
            return true;

        final Configuration newConfiguration = ModuleLayer.boot().configuration().resolve(
                ModuleFinder.ofSystem(),
                ModuleFinder.of(),
                Set.of(VECTOR_MODULE_NAME)
        );

        final Function<String, ClassLoader> clf;

        {
            final MethodHandles.Lookup IMPL_LOOKUP = ImplLookupGetter.DEFAULT_IMPL.getTrustedLookup();
            if (IMPL_LOOKUP == null) return false;

            final Class<?> moduleLoaderMapClass;
            final MethodHandle mappingFunctionMH;
            try {
                moduleLoaderMapClass = Class.forName("jdk.internal.module.ModuleLoaderMap", false, null);
            } catch (ClassNotFoundException e) {
                return false;
            }
            try {
                mappingFunctionMH = IMPL_LOOKUP.findStatic(moduleLoaderMapClass, "mappingFunction", MethodType.methodType(Function.class, Configuration.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                final Function<String, ClassLoader> _clf = (Function<String, ClassLoader>) mappingFunctionMH.invokeExact(newConfiguration);
                clf = _clf;
            } catch (Throwable e) {
                return false;
            }
        }

        final ModuleLayer newLayer = ModuleLayer.boot().defineModules(newConfiguration, clf);
        if (!newLayer.findModule(VECTOR_MODULE_NAME).isPresent())
            return false;
        VECTOR_LOADED.setTarget(TRUE_MH);
        return true;
    }
}
