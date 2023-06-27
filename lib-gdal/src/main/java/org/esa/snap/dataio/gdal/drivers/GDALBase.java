package org.esa.snap.dataio.gdal.drivers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public abstract class GDALBase {

    protected MethodHandle createStaticHandle(Class<?> implClass, String method, Class<?> returnType, Class<?>... argTypes) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(returnType, argTypes);
        return lookup.findStatic(implClass, method, methodType).asSpreader(Object[].class, argTypes.length);
    }

    protected MethodHandle createHandle(Class<?> implClass, String method, Class<?> returnType, Class<?>... argTypes) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(returnType, argTypes);
        return lookup.findVirtual(implClass, method, methodType).asSpreader(Object[].class, argTypes.length + 1);
    }

    protected static Object invokeStatic(MethodHandle handle, Object... args) {
        try {
            return handle.invoke(args);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected static Object invoke(MethodHandle handle, Object instance, Object... args) {
        try {
            return handle.invoke(prepareArguments(instance, args));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] prepareArguments(Object instance, Object[] arguments) {
        final Object[] newArgs;
        if (instance != null) {
            newArgs = new Object[arguments.length + 1];
            newArgs[0] = instance;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
        } else {
            newArgs = arguments;
        }
        return newArgs;
    }
}
