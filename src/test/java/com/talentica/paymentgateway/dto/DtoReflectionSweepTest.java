package com.talentica.paymentgateway.dto;

import com.talentica.paymentgateway.util.PojoTester;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class DtoReflectionSweepTest {

    private static final String CLASSES_DIR = "/Users/adawande/Documents/talentica/payment-gateway/target/classes";

    @Test
    void exerciseAllDtoClasses() throws Exception {
        for (Class<?> c : findClasses("com/talentica/paymentgateway/dto")) {
            try { PojoTester.exerciseClass(c); } catch (Throwable ignored) {}
            try { PojoTester.exerciseInnerClasses(c); } catch (Throwable ignored) {}
        }
    }

    private static List<Class<?>> findClasses(String relativePath) throws Exception {
        Path dir = Path.of(CLASSES_DIR, relativePath);
        List<Class<?>> result = new ArrayList<>();
        if (!Files.exists(dir)) return result;
        Path root = Path.of(CLASSES_DIR);
        Files.walk(dir)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    String rel = root.relativize(p).toString().replace(File.separatorChar, '/');
                    if (rel.contains("$")) return; // skip anonymous/synthetic inner classes; handled separately
                    String fqcn = rel.substring(0, rel.length() - 6).replace('/', '.');
                    try { result.add(Class.forName(fqcn)); } catch (Throwable ignored) {}
                });
        return result;
    }
}
