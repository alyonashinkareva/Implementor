package info.kgeorgiy.ja.shinkareva.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class implements {@link JarImpler} interfaces.
 * @author Shinkareva Alyona (alyona.i.shinkareva@gmail.com)
 */

public class Implementor implements JarImpler {
    /**
     * Line separator constant.
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    /**
     * Left brace constant.
     */
    private static final String LEFT_BRACE = "{";
    /**
     * Right brace constant.
     */
    private static final String RIGHT_BRACE = "}";

    /**
     * Coma constant.
     */
    private static final String COMMA = ",";
    /**
     * "Impl" - end of the classname - constant.
     */
    private static final String CLASS_SUFFIX = "Impl";
    /**
     * Space constant.
     */
    private static final String SPACE = " ";
    /**
     * Semicolon constant.
     */
    private static final String SEMICOLON = ";";
    /**
     * Dot constant.
     */
    private static final String DOT = ".";
    /**
     * Java file extension constant.
     */
    private static final String JAVA_FILE_EXTENSION = ".java";

    /**
     * Constructs a new implementor.
     */
    public Implementor() {
    }

    /**
     * Produces code implementing class or interface specified by provided {@code aClass}.
     * @param aClass type token to create implementation for.
     * @param path root directory.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (!checkClass(aClass)) {
            throw new ImplerException("Incorrect class");
        }
        Path realPath = path.resolve(aClass.getPackageName().replace(DOT, File.separator));
        createDirectory(realPath);
        try (BufferedWriter writer = Files.newBufferedWriter(realPath.resolve(aClass.getSimpleName() + CLASS_SUFFIX + JAVA_FILE_EXTENSION))) {
            writer.write(toUnicode(createClassHeader(aClass)));
            if (!aClass.isInterface()) {
                for (Constructor<?> constructor : generateConstructors(aClass)) {
                    writer.write(toUnicode("     " + generateCode(constructor)));
                }
            }
            for (MyMethod method : generateMethods(aClass)) {
                writer.write(toUnicode(generateCode(method.method)));
            }
            writer.write(toUnicode(RIGHT_BRACE + LINE_SEPARATOR));
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Converts given string to Unicode
     * @param str {@link String} to convert
     * @return converted string
     */
    private String toUnicode(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= 128) {
                result.append("\\u").append(Integer.toHexString(c | 0x10000).substring(1));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Generates a string representation of header for class or interface specified by provided {@code aClass}.
     * @param aClass input class.
     * @return result of header generation.
     */
    private String createClassHeader(Class<?> aClass) {
        StringBuilder sb = new StringBuilder();
        String aPackage = aClass.getPackageName();
        if (!aPackage.isEmpty()) {
            sb.append("package ")
                    .append(aPackage)
                    .append(SEMICOLON)
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        sb.append("public class ")
                .append(aClass.getSimpleName())
                .append(CLASS_SUFFIX)
                .append(SPACE)
                .append(aClass.isInterface() ? "implements " : "extends ")
                .append(aClass.getCanonicalName())
                .append(SPACE)
                .append(LEFT_BRACE)
                .append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Returns a string representation of modifiers of method or constructor specified by provided {@code executable}.
     * @param executable input method or constructor.
     * @return string of modifiers.
     */
    private String generateModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT) + SPACE;
    }

    /**
     * Returns a string representation of parameters of method or constructor specified by provided {@code executable}.
     * @param executable input method or constructor.
     * @param withName is true if method has to return types of parameters.
     * @return string of parameters.
     */

    private String generateParameters(Executable executable, boolean withName) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> (withName ? parameter.getType().getCanonicalName() + SPACE : "") + parameter.getName())
                .collect(Collectors.joining(COMMA + SPACE, "(", ")"));
    }

    /**
     * Generates a string representation of the exceptions thrown by method or constructor specified by provided {@code executable}.
     * @param executable input method or constructor.
     * @return string of exceptions.
     */
    private String generateExceptions(Executable executable) {
        StringBuilder sb = new StringBuilder();
        var exceptions = executable.getExceptionTypes();
        if (exceptions.length != 0) {
            sb.append(" throws ").append(
                    Arrays.stream(exceptions)
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining(COMMA + SPACE))
            );
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of the return + default value of method specified by provided {@code method}
     * @param method input method.
     * @return string of the return + default value
     */

    private String generateReturn(Method method) {
        if (method.getReturnType().equals(boolean.class)) {
            return "return true";
        } else if (method.getReturnType().equals(void.class)) {
            return "return";
        } else if (method.getReturnType().isPrimitive()) {
            return "return 0";
        }
        return "return null";
    }

    /**
     * Returns a string representation of generated code of method or constructor specified by provided {@code executable}.
     * @param executable input method or constructor.
     * @return string of generated code.
     */

    private String generateCode(Executable executable) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateModifiers(executable));
        if (executable instanceof Method m) {
            sb.append(m.getReturnType().getCanonicalName())
                    .append(SPACE)
                    .append(m.getName());
        } else {
            sb.append(((Constructor<?>) executable).getDeclaringClass().getSimpleName()).append(CLASS_SUFFIX);
        }
        sb.append(SPACE).append(generateParameters(executable, true))
                .append(generateExceptions(executable))
                .append(LEFT_BRACE)
                .append(LINE_SEPARATOR)
                .append("        ")
                .append((executable instanceof Method) ? generateReturn((Method) executable) : "super " + generateParameters(executable, false))
                .append(SEMICOLON)
                .append(LINE_SEPARATOR)
                .append(SPACE)
                .append(RIGHT_BRACE)
                .append(LINE_SEPARATOR);
        return sb.toString();
    }

    /**
     * Returns list of constructors of class specified by provided {@code aClass}.
     * @param aClass type token to return constructor for.
     * @return list of constructors.
     */
    private List<Constructor<?>> generateConstructors(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());
    }

    /**
     * Returns set of methods of class or interface specified by provided {@code aClass}.
     * @param aClass type token to return methods for.
     * @return set of methods.
     */
    private Set<MyMethod> generateMethods(Class<?> aClass) {
        var methods = Arrays.stream(aClass.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MyMethod::new)
                .collect(Collectors.toSet());
        while (aClass != null) {
            Arrays.stream(aClass.getDeclaredMethods())
                    .filter(method -> Modifier.isAbstract(method.getModifiers()))
                    .map(MyMethod::new)
                    .collect(Collectors.toCollection(() -> methods));
            aClass = aClass.getSuperclass();
        }
        return methods;
    }

    /**
     * Returns {@code true} if token is valid or {@code false} if not.
     * <p>
     * Token valid if not:
     * <ul>
     *     <li>Record</li>
     *     <li>Primitive</li>
     *     <li>Enum</li>
     *     <li>Completions</li>
     *     <li>Final modifier</li>
     *     <li>Private modifier</li>
     *     </ul>
     * @param aClass type token to check.
     * @return {@code true} if token is valid or {@code false} if not.
     */
    private boolean checkClass(Class<?> aClass) {
        final int modifiers = aClass.getModifiers();

        return !(aClass == java.lang.Record.class ||
                aClass.isPrimitive() ||
                aClass == Enum.class ||
                aClass == javax.annotation.processing.Completions.class ||
                Modifier.isFinal(modifiers) ||
                Modifier.isPrivate(modifiers)
        );
    }

    /**
     * Creates directory for {@code path}
     * @param path directory to create.
     * @throws ImplerException if it could not create a directory.
     */
    private void createDirectory(Path path) throws ImplerException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ImplerException("Could not create directory: " + e.getMessage());
        }
    }

    /**
     * Produces <var>.jar</var> file containing code implementation generated via {@link Implementor#implement(Class, Path)}
     * @param aClass type token to create implementation for.
     * @param path target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        if (checkClass(aClass)) {
            implement(aClass, path.getParent());
            compile(aClass, path.getParent());
            try {
                createJar(aClass, path);
            } catch (IOException e) {
                throw new ImplerException(e.getMessage());
            }
        } else {
            throw new ImplerException("Incorrect class");
        }
    }

    /**
     * Returns the implementation name for a given class token.
     *
     * @param token the Class token
     * @return the implementation name as a String
     */

    private static String getImplName(final Class<?> token) {
        return token.getPackageName() + DOT + token.getSimpleName() + CLASS_SUFFIX;
    }

    /**
     * Returns the file path for a given class in a specified root directory.
     *
     * @param root the root directory Path
     * @param clazz the Class for which the file path is needed
     * @return the Path object representing the file path
     */

    public static Path getFile(final Path root, final Class<?> clazz) {
        return root.resolve(getImplName(clazz).replace(DOT, File.separator) + JAVA_FILE_EXTENSION).toAbsolutePath();
    }

    /**
     * Returns the class path for a given Class object.
     *
     * @param aClass the Class object
     * @return the class path as a String
     */

    private static String getClassPath(final Class<?> aClass) {
        try {
            return Path.of(aClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Compiles generated class by given token specified by provided {@code aClass}
     * @param aClass given token.
     * @param path given path.
     * @throws ImplerException when the given class could not be compiled.
     */
    private void compile(Class<?> aClass, Path path) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = path.toString() + File.pathSeparator + getClassPath(aClass);
        String[] args = new String[]{getFile(path, aClass).toString(),
                "-cp",
                classpath,
                "-encoding",
                "UTF8"
        };
        if (compiler == null || compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Compiler exit code");
        }
    }

    /**
     * Creates <var>.jar</var> file for class by given token specified by provided {@code aClass}
     * @param aClass given token.
     * @param path given path to <var>.jar</var> file.
     * @throws IOException when I/O exception occurred.
     */

    private void createJar(Class<?> aClass, Path path) throws IOException {
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(path), new Manifest());
            var name = aClass.getPackageName().replace(DOT.charAt(0), '/')
                    + '/'
                    + aClass.getSimpleName()
                    + CLASS_SUFFIX + ".class";
            jarOutputStream.putNextEntry(new ZipEntry(name));
            Files.copy(path.getParent().resolve(name), jarOutputStream);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Class which stores the method with custom equals and hashCode.
     * Can be used in set.
     */
    private static class MyMethod {
        /**
         * Method.
         */
        private final Method method;

        /**
         * Constructor pf MyMethod
         * @param method given method.
         */

        MyMethod(Method method) {
            this.method = method;
        }

        /**
         * Compares {@link #method} with given object and returns {@code true} if equal or {@code false} if not.
         * @param object to compare with.
         * @return {@code true} if equal or {@code false} if not.
         */

        public boolean equals(Object object) {
            if (object == null) {
                return false;
            } else if (object instanceof MyMethod) {
                return object.hashCode() == hashCode();
            }
            return false;
        }

        /**
         * Calculates and returns hash of {@link #method}
         * @return hash of {@link #method}
         */

        @Override
        public int hashCode() {
            return (method.getReturnType().hashCode() + method.getName().hashCode()) * 103 + Arrays.hashCode(method.getParameterTypes());
        }
    }

    /**
     * The function, which is used to choose what implementation is expected to be used.
     *
     * @param args The command line arguments. Should contain either 2 or 3 arguments:
     * <ul>
     * <li> If 2 arguments are provided, expects {@code className rootPath} amd runs {@link #implement(Class, Path)}.</li>
     * <li> If 3 arguments are provided, expects {@code -jar className jarPath} and runs {@link #implementJar(Class, Path)}.</li>
     * </ul>
     * If error occurs or arguments are wrong, prints error information message.
     */

    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Please enter: <classname> <root directory of output file>   OR   -jar <classname> <path to output .jar file>");
            return;
        }

        Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (!Objects.equals(args[0], "-jar")) { // FIXED :NOTE: стоит проверить, что 3 параметр именно -jar?
                System.out.println("If you want to call JarImplementer, first argument must be '-jar'");
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Not valid class name: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println(e.getMessage());
        }
    }
}