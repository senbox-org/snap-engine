package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.core.runtime.internal.IOHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that tries to resolve (i.e. expand) the system variables values.
 *
 * @author Cosmin Cara
 */
public abstract class VariableResolver {

    private static Map<ToolAdapterOperatorDescriptor, VariableResolver> resolverMap = new HashMap<>();

    VariableResolver nextResolver;
    protected ToolAdapterOperatorDescriptor descriptor;

    public static VariableResolver newInstance(ToolAdapterOperatorDescriptor descriptor) {
        if (!resolverMap.containsKey(descriptor)) {
            VariableResolver simpleResolver = new SimpleVariableResolver(descriptor);
            VariableResolver systemPathResolver = new SystemPathVariableResolver(descriptor);
            simpleResolver.setNextResolver(systemPathResolver);
            resolverMap.put(descriptor, simpleResolver);
        }
        return resolverMap.get(descriptor);
    }

    VariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
        this.descriptor = operatorDescriptor;
    }

    public abstract String resolveString(String input);
    public abstract Path resolve(String input);
    private void setNextResolver(VariableResolver resolver) {
        this.nextResolver = resolver;
    }
    boolean isValidFileName(String fileName) {
        Path f = Path.of(fileName);
        try {
            //noinspection ResultOfMethodCallIgnored
            f.toRealPath().toString();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    String recursiveResolve(String input) {
        int maxLevels = 3;
        Map<String, String> lookupVars = this.descriptor.getVariables()
                .stream()
                .collect(Collectors.toMap(SystemVariable::getKey,
                                          (systemVariable) -> systemVariable.getValue() != null ?
                                                  systemVariable.getValue() : ""));
        while (input.contains("$") && maxLevels > 0) {
            for (String key : lookupVars.keySet()) {
                input = input.replace("$" + key, lookupVars.get(key));
            }
            maxLevels--;
        }
        return input;
    }

    /**
     * Resolver class for substituting variables with their values.
     */
    static class SimpleVariableResolver extends VariableResolver {

        SimpleVariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
            super(operatorDescriptor);
        }

        @Override
        public String resolveString(String input) {
            String resolved = input;
            if (input != null) {
                resolved = recursiveResolve(resolved);
                if (nextResolver != null && isValidFileName(resolved)) {
                    resolved = nextResolver.resolveString(resolved);
                }
            }
            return resolved;
        }

        @Override
        public Path resolve(String input) {
            if (input == null) {
                return null;
            }
            Path resolved = Path.of(input);
            if (!Files.exists(resolved)) {
                String expandedValue = recursiveResolve(resolved.toString());
                resolved = Path.of(expandedValue);
                if (nextResolver != null && !Files.exists(resolved)) {
                    resolved = nextResolver.resolve(resolved.toString());
                }
            }
            return resolved;
        }
    }

    /**
     * Resolver class for expanding possible system paths.
     */
    static class SystemPathVariableResolver extends VariableResolver {

        private final String[] systemPath;

        SystemPathVariableResolver(ToolAdapterOperatorDescriptor operatorDescriptor) {
            super(operatorDescriptor);
            String sysPath = System.getenv("PATH");
            systemPath = sysPath.split(IOHelper.PATH_SEPARATOR);
        }

        @Override
        public String resolveString(String input) {
            String resolved = input;
            if (resolved != null) {
                Path resolvedFile = resolve(input);
                if (resolvedFile != null) {
                    resolved = resolvedFile.toAbsolutePath().toString();
                }
            }
            return resolved;
        }

        @Override
        public Path resolve(String input) {
            if (input == null) {
                return null;
            }
            Path resolved = Path.of(input);
            if (!Files.exists(resolved)) {
                for (String sysPath : systemPath) {
                    Path current = Path.of(sysPath).resolve(resolved.toString());
                    if (Files.exists(current)) {
                        resolved = current;
                        break;
                    }
                }
                if (nextResolver != null) {
                    resolved = nextResolver.resolve(input);
                }
            }
            return resolved;
        }
    }
}
