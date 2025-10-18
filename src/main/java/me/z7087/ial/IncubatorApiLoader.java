package me.z7087.ial;

import me.z7087.ilg.ImplLookupGetter;

import java.lang.ModuleLayer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class IncubatorApiLoader {

    private static final BooleanSupplier FALSE_SUPPLIER = () -> false;
    private static final IncubatorApiLoader NOT_SUPPORTED_LOADER = new IncubatorApiLoader(null, FALSE_SUPPLIER);

    private final String moduleName;
    private boolean isLoaded;
    private final BooleanSupplier isLoadedSupplier;

    private IncubatorApiLoader(String moduleName, BooleanSupplier isLoadedSupplier) {
        this.moduleName = moduleName;
        this.isLoadedSupplier = isLoadedSupplier;
    }

    @SuppressWarnings("Since15")
    public static IncubatorApiLoader of(String moduleName, String classNameInModule) {
        if (!IS_MODULE_SUPPORTED)
            return NOT_SUPPORTED_LOADER;
        if (ModuleFinder.ofSystem().find(moduleName).isPresent()) {
            return new IncubatorApiLoader(moduleName, new BooleanSupplier() {
                private boolean isLoaded;
                @Override
                public boolean getAsBoolean() {
                    if (isLoaded)
                        return true;
                    try {
                        Class.forName(classNameInModule, false, null);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                    isLoaded = true;
                    return true;
                }
            });
        }
        return NOT_SUPPORTED_LOADER;
    }

    @SuppressWarnings("Since15")
    public static IncubatorApiLoader of(String moduleName, BooleanSupplier isLoadedSupplier) {
        if (!IS_MODULE_SUPPORTED)
            return NOT_SUPPORTED_LOADER;
        if (ModuleFinder.ofSystem().find(moduleName).isPresent()) {
            return new IncubatorApiLoader(moduleName, isLoadedSupplier);
        }
        return NOT_SUPPORTED_LOADER;
    }

    public boolean isLoaded() {
        if (!IS_MODULE_SUPPORTED)
            return false;
        if (isLoaded)
            return true;
        if (!isLoadedSupplier.getAsBoolean())
            return false;
        isLoaded = true;
        return true;
    }

    public boolean isSupported() {
        if (!IS_MODULE_SUPPORTED)
            return false;
        return this != IncubatorApiLoader.NOT_SUPPORTED_LOADER;
    }

    @SuppressWarnings("Since15")
    public boolean tryLoad() {
        if (!IS_MODULE_SUPPORTED)
            return false;
        if (!isSupported())
            return false;
        if (isLoaded())
            return true;

        final Configuration newConfiguration = ModuleLayer.boot().configuration().resolve(
                ModuleFinder.ofSystem(),
                ModuleFinder.of(),
                Set.of(moduleName)
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
        if (!newLayer.findModule(moduleName).isPresent())
            return false;
        isLoaded = true;
        return true;
    }

    private static final boolean IS_MODULE_SUPPORTED;

    static {
        boolean isModuleSupported;
        try {
            Class.forName("java.lang.Module", false, null);
            isModuleSupported = true;
        } catch (ClassNotFoundException e) {
            isModuleSupported = false;
        }
        IS_MODULE_SUPPORTED = isModuleSupported;
    }
}
