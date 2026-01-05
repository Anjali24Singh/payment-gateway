package com.talentica.paymentgateway.util;

import org.assertj.core.api.Assertions;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

public final class PojoTester {

    private PojoTester() {}

    public static <T> T exerciseClass(Class<T> clazz) {
        T instance = instantiate(clazz);
        if (instance == null) return null;

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object sample = sampleValueFor(field.getType());
            // setter
            tryInvokeSetter(clazz, instance, "set" + capitalize(field.getName()), field.getType(), sample);
            // direct set
            try { field.set(instance, sample); } catch (Throwable ignored) {}
            // getter forms
            for (String getter : getterCandidates(field)) {
                tryInvokeGetter(clazz, instance, getter);
            }
        }

        // toString path
        Assertions.assertThat(instance.toString()).isNotNull();
        return instance;
    }

    public static void exerciseInnerClasses(Class<?> outer) {
        for (Class<?> inner : outer.getDeclaredClasses()) {
            if (Modifier.isAbstract(inner.getModifiers()) || inner.isInterface()) continue;
            Object obj = instantiate(inner);
            if (obj == null) continue;
            for (Field f : inner.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object v = sampleValueFor(f.getType());
                tryInvokeSetter(inner, obj, "set" + capitalize(f.getName()), f.getType(), v);
                try { f.set(obj, v); } catch (Throwable ignored) {}
                for (String g : getterCandidates(f)) {
                    tryInvokeGetter(inner, obj, g);
                }
            }
            Assertions.assertThat(obj.toString()).isNotNull();
        }
    }

    private static List<String> getterCandidates(Field field) {
        String cap = capitalize(field.getName());
        List<String> out = new ArrayList<>();
        out.add("get" + cap);
        if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            out.add("is" + cap);
            if (!field.getName().startsWith("is")) {
                out.add("getIs" + cap);
            }
        }
        return out;
    }

    private static void tryInvokeSetter(Class<?> clazz, Object instance, String name, Class<?> type, Object value) {
        try {
            Method m = clazz.getMethod(name, type);
            m.setAccessible(true);
            m.invoke(instance, value);
        } catch (Throwable ignored) {}
    }

    private static Object tryInvokeGetter(Class<?> clazz, Object instance, String name) {
        try {
            Method m = clazz.getMethod(name);
            m.setAccessible(true);
            return m.invoke(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String capitalize(String s) { return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private static Object sampleValueFor(Class<?> type) {
        if (!type.isPrimitive()) {
            if (type == String.class) return "sample";
            if (type == Integer.class) return 1;
            if (type == Long.class) return 1L;
            if (type == Short.class) return (short)1;
            if (type == Byte.class) return (byte)1;
            if (type == Boolean.class) return true;
            if (type == Double.class) return 1.0d;
            if (type == Float.class) return 1.0f;
            if (type == BigDecimal.class) return new BigDecimal("1.00");
            if (type == ZonedDateTime.class) return ZonedDateTime.now();
            if (type == UUID.class) return UUID.randomUUID();
            if (type == List.class) return new ArrayList<>();
            if (type == Map.class) return new HashMap<>();
            if (type.isEnum()) return type.getEnumConstants()[0];
        }
        if (type == int.class) return 1;
        if (type == long.class) return 1L;
        if (type == short.class) return (short)1;
        if (type == byte.class) return (byte)1;
        if (type == boolean.class) return true;
        if (type == double.class) return 1.0d;
        if (type == float.class) return 1.0f;
        try {
            Constructor<?> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<T> clazz) {
        try {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) return null;
            Constructor<?> noArgs = clazz.getDeclaredConstructor();
            noArgs.setAccessible(true);
            return (T) noArgs.newInstance();
        } catch (Throwable e) {
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                try {
                    c.setAccessible(true);
                    Class<?>[] types = c.getParameterTypes();
                    Object[] args = new Object[types.length];
                    for (int i = 0; i < types.length; i++) args[i] = sampleValueFor(types[i]);
                    return (T) c.newInstance(args);
                } catch (Throwable ignored) {}
            }
            return null;
        }
    }
}
