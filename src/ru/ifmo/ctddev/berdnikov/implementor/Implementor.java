package ru.ifmo.ctddev.berdnikov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;


/**
 * Class provides methods for generating implementation of class which implements
 * or extends given interface or class, and for packing this implementation in given <tt>.jar</tt> file.
 * <p>
 * Implementation can be generated via calling {@link #implement(Class, java.io.File)} or
 * {@link #main(String[])} with following arguments: <p><tt>full.[class/interface].name</tt>
 * <p>
 * Jar file can be generated via calling {@link #implementJar(Class, java.io.File)} or
 * {@link #main(String[])} with following arguments: <p><tt>-jar full.[class/interface].name [jar-name]</tt>
 *
 * @see #implement(Class, java.io.File)
 * @see #implementJar(Class, java.io.File)
 * @see #main(String[])
 */
public class Implementor implements JarImpler {
    /**
     * Is used by {@link #implementJar(Class, java.io.File)}.
     * Contains path to generated <tt>.java</tt> file after
     * execution of {@link #implement(Class, java.io.File)} method.
     */
    private Path targetPath;
    /**
     * Is used by {@link #implementJar(Class, java.io.File)}.
     * After execution of {@link #implement(Class, java.io.File)} method
     * contains path to <tt>.class</tt> file, which will be created
     * after compiling corresponding <tt>.java</tt> file.
     */
    private Path targetClassPath;

    /**
     * Is used by {@link #main(String[])} to call {@link #run(String[])} method.
     */
    public Implementor() {}

    /**
     * Generates implementation of class which implements or extends given interface or class.
     *
     * @param inputClass class or interface to implement
     * @return {@link java.lang.StringBuilder} which contains a correct java code of class
     * @throws ImplerException if class can't be generated
     */
    private StringBuilder generateClass(Class<?> inputClass) throws ImplerException {
        StringBuilder out = new StringBuilder();
        out.append(String.format("package %s;%n%n", inputClass.getPackage().getName()));
        String implName = inputClass.isInterface() ? "implements" : "extends";
        String className = inputClass.getSimpleName() + "Impl";
        out.append(String.format("public class %s %s %s {%n", className, implName, inputClass.getName()));

        if (!inputClass.isInterface()) {
            Constructor<?>[] constructors = inputClass.getConstructors();
            if (constructors.length == 0) {
                constructors = inputClass.getDeclaredConstructors();
            }
            if (constructors.length == 0) {
                throw new ImplerException();
            } else {
                boolean needOverride = true;
                Constructor<?> constructorToOverride = null;
                for (Constructor<?> constructor : constructors) {
                    if (Modifier.isPrivate(constructor.getModifiers())) {
                        continue;
                    }
                    if (constructor.getParameters().length == 0) {
                        needOverride = false;
                    } else {
                        constructorToOverride = constructor;
                    }
                }
                if (needOverride) {
                    if (constructorToOverride == null) {
                        throw new ImplerException();
                    } else {
                        out.append(generateConstructor(constructorToOverride, className));
                    }
                }
            }
        }

        Set<Method> set = new TreeSet<>(comparator);
        walk(inputClass.getMethods(), set, out);
        if (!inputClass.isInterface()) {
            Class<?> cl = inputClass;
            while (cl != null) {
                walk(cl.getDeclaredMethods(), set, out);
                cl = cl.getSuperclass();
            }
        }

        out.append(String.format("}%n"));
        return out;
    }


    /**
     * Generates code for each abstract method in given array,
     * if method is not presented in given set, and append it to given StringBuilder.
     * Is used by generateClass() to append method implementations to class.
     *
     * @param methods array of methods to generate
     * @param set     set of methods which are already generated
     * @param out     StringBuilder to which append generated code
     */
    private void walk(Method[] methods, Set<Method> set, StringBuilder out) {
        for (Method m : methods) {
            if (set.add(m)) {
                if (Modifier.isAbstract(m.getModifiers())) {
                    out.append(generateMethod(m));
                }
            }
        }
    }


    /**
     * Is used by {@link #generateClass(Class)} to identify whether implementations of
     * given methods can be found in one class.
     * First they are compared by name,
     * then by number of parameters, then by type names.
     */
    private static final Comparator<Method> comparator = (m1, m2) -> {
        String name1 = m1.getName();
        String name2 = m2.getName();
        int ret = name1.compareTo(name2);
        if (ret != 0) {
            return ret;
        }
        Class<?>[] pt1 = m1.getParameterTypes();
        Class<?>[] pt2 = m2.getParameterTypes();
        ret = Integer.compare(pt1.length, pt2.length);
        if (ret != 0) {
            return ret;
        }
        for (int i = 0; i < pt1.length; i++) {
            ret = pt1[i].getTypeName().compareTo(pt2[i].getTypeName());
            if (ret != 0) {
                return ret;
            }
        }
        return 0;
    };


    /**
     * Generates piece of declaration of given Executable (method or constructor) and
     * appends it to given StringBuilder.
     * Is written to avoid copypasting. Is used by {@link #generateConstructor(java.lang.reflect.Constructor, String)}
     * and by {@link #generateMethod(java.lang.reflect.Method)} to generate a piece of declaration which contains
     * parameters and exceptions.
     *
     * @param ex  method or constructor which declaration will be generated
     * @param out {@link java.lang.StringBuilder} to which the result will be append
     * @return {@link java.lang.StringBuilder} with parameter's names separated
     * by commas. Is used by {@link #generateConstructor(java.lang.reflect.Constructor, String)}
     * to insert in super constructor calling.
     */
    private StringBuilder insertParametersAndExceptions(Executable ex, StringBuilder out) {
        out.append('(');
        Class<?>[] parameterTypes = ex.getParameterTypes();
        StringBuilder params = new StringBuilder();
        if (parameterTypes.length > 0) {
            int l = parameterTypes.length;
            for (int i = 0; i < l - 1; i++) {
                out.append(String.format("%s p%d, ", parameterTypes[i].getTypeName(), i));
                params.append(String.format("p%s, ", i));
            }
            if (ex.isVarArgs()) {
                String typeName = parameterTypes[l - 1].getTypeName();
                out.append(String.format("%s... p%d", typeName.substring(0, typeName.length() - 2), l - 1));
            } else {
                out.append(String.format("%s p%d", parameterTypes[l - 1].getTypeName(), l - 1));
            }
            params.append(String.format("p%d", l - 1));
        }
        out.append(')');
        Class<?>[] exceptionTypes = ex.getExceptionTypes();
        if (exceptionTypes.length > 0) {
            out.append(String.format(" throws %s", exceptionTypes[0].getName()));
            for (int i = 1; i < exceptionTypes.length; i++) {
                out.append(String.format(", %s", exceptionTypes[i].getTypeName()));
            }
        }
        return params;
    }

    /**
     * Generates implementation of given constructor.
     * Is used by {@link #generateClass(Class)} to insert generated
     * implementation in class.
     *
     * @param constructor constructor to generate
     * @param name        name of class in which generated constructor will be placed
     * @return StringBuilder with generated implementation
     */
    private StringBuilder generateConstructor(Constructor<?> constructor, String name) {
        StringBuilder out = new StringBuilder();
        int modifiers = constructor.getModifiers() & ~Modifier.ABSTRACT & Modifier.constructorModifiers();
        out.append(String.format("%s %s", Modifier.toString(modifiers), name));
        StringBuilder params = insertParametersAndExceptions(constructor, out);
        out.append(String.format(" {%nsuper(%s);%n}%n", params));
        return out;
    }

    /**
     * Generates implementation of given method.
     * Is used by {@link #generateClass(Class)} in {@link #walk(java.lang.reflect.Method[], java.util.Set, StringBuilder)}
     * to insert generated implementation in class.
     *
     * @param m method to generate
     * @return StringBuilder with generated implementation
     */
    private StringBuilder generateMethod(Method m) {
        StringBuilder out = new StringBuilder();
        String name = m.getName();
        Class<?> returnType = m.getReturnType();
        int modifiers = m.getModifiers() & ~Modifier.ABSTRACT & Modifier.methodModifiers();
        out.append(String.format("%s %s %s", Modifier.toString(modifiers), returnType.getTypeName(), name));
        insertParametersAndExceptions(m, out);
        out.append(String.format(" {%n"));
        if (!returnType.equals(void.class)) {
            String ret = "null";
            if (returnType.isArray()) {
                String retString = returnType.getTypeName();
                ret = String.format("new %s0%s",
                        retString.substring(0, retString.indexOf('[') + 1),
                        retString.substring(retString.indexOf('[') + 1));
            } else if (returnType.isPrimitive()) {
                if (returnType.equals(boolean.class)) {
                    ret = "false";
                } else {
                    ret = "0";
                }
            }
            out.append(String.format("return %s;%n", ret));
        }
        out.append(String.format("}%n"));
        return out;
    }

    /**
     * Prints usage to System.err.
     * <p>
     * Prints the following lines:
     * <tt>Usage: java ru.ifmo.ctddev.berdnikov.implementor.Implementor full.[class/interface].name</tt>
     * <p>
     * <tt>or: java ru.ifmo.ctddev.berdnikov.implementor.Implementor -jar full.[class/interface].name [jar-name]</tt>
     */
    private void printUsage() {
        System.err.format("Usage: java %s full.[class/interface].name%n" +
                "   or: java %1$s -jar full.[class/interface].name [jar-name]%n", getClass().getName());
    }

    /**
     * Checks given args for correctness.
     * Args are correct only when they can be used by program
     *
     * @param args arguments for checking
     * @return true if args are correct, false otherwise
     */
    private boolean checkArgs(String[] args) {
        String errorText = null;

        if (args == null) {
            errorText = "no args";
        } else if (args.length != 1 && args.length != 3) {
            errorText = "wrong number of args";
        } else if (args[0] == null || args.length == 3 && (args[1] == null || args[2] == null)) {
            errorText = "null in args";
        } else if (args.length == 3 && !args[0].equals("-jar")) {
            errorText = "wrong parameters";
        }

        if (errorText != null) {
            System.err.format("Error: %s%n", errorText);
            printUsage();
            return false;
        }
        return true;
    }

    /**
     * Is used by {@link #main(String[])} to avoid providing
     * {@code static} for every method
     *
     * @param args arguments which method {@link #main(String[])} provides
     * @throws ImplerException if implementation can't be generated
     */
    public void run(String[] args) throws ImplerException {
        if (!checkArgs(args)) {
            return;
        }

        try {
            if (args.length == 1) {
                implement(Class.forName(args[0]), new File(System.getProperty("user.dir")));
            } else {
                implementJar(Class.forName(args[1]), new File(args[2]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println(String.format("Error: class not found - %s%n", e.getMessage()));
        }
    }

    /**
     * Main method is used to run program from console.
     * Implementation can be generated via calling with following arguments:
     * <p><tt>full.[class/interface].name</tt>
     * <p>
     * Jar file can be generated via calling with following arguments:
     * <p><tt>-jar full.[class/interface].name [jar-name]</tt>
     *
     * @param args arguments for program
     * @throws ImplerException if class can't be implemented
     *
     * @see #run(String[])
     */
    public static void main(String[] args) throws ImplerException {
        new Implementor().run(args);
    }


    /**
     * Checks if some args are null.
     * Simply iterates through given array of Objects and returns {@code true}
     * if encounters with {@code null}
     *
     * @param args arguments for checking
     * @return false if some arguments are null, true otherwise
     */
    private boolean checkNull(Object... args) {
        for (Object o : args) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if given class can't be implemented.
     *
     * @param inputClass class of interface for checking
     * @return {@code false} - if class is primitive or is array or is final, {@code true} - otherwise
     */
    private boolean canGenerate(Class<?> inputClass) {
        return !(inputClass.isPrimitive()
                || inputClass.isArray()
                || Modifier.isFinal(inputClass.getModifiers()));
    }

    /**
     * Produces code implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <tt>root</tt> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <tt>$root/java/util/ListImpl.java</tt>
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be
     *                                                                 generated.
     */
    @Override
    public void implement(Class<?> token, File root) throws ImplerException {
        if (checkNull(token, root)) {
            System.err.println(String.format("Error: null in args%n-- aClass == %s%n-- file == %s%n", token, root));
            throw new ImplerException();
        }
        if (!canGenerate(token)) {
            throw new ImplerException();
        }
        Path rootDir = root.toPath();
        Path packageDir = Paths.get(
                token.getPackage()
                        .getName()
                        .replace(".", File.separator)
        );
        targetClassPath = packageDir;
        Path dir = rootDir.resolve(packageDir);
        try {
            Files.createDirectories(dir);
            targetClassPath = targetClassPath.resolve(token.getSimpleName() + "Impl.class");
            dir = dir.resolve(token.getSimpleName() + "Impl.java");
            targetPath = dir;
            try (Writer writer = Files.newBufferedWriter(dir)) {
                writer.write(generateClass(token).toString());
            } catch (SecurityException | UnsupportedOperationException | IOException e) {
                System.err.format("Error while creating file or writing to it: %s%n", e.toString());
                throw new ImplerException();
            }
        } catch (FileAlreadyExistsException e) {
            System.err.format("Error: can`t create directory, " +
                    "because %s already exists and isn`t directory%n", e.getFile());
            throw new ImplerException();
        } catch (AccessDeniedException e) {
            System.err.format("Error: access denied, can`t create %s%n", e.getFile());
            throw new ImplerException();
        } catch (SecurityException | UnsupportedOperationException | IOException e) {
            System.err.format("Error while creating directory: %s%n", e.toString());
            throw new ImplerException();
        }
    }

    /**
     * Delete given file or directory.
     *
     * @param file file or directory to delete
     */
    private void clean(final File file) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File child : files) {
                    clean(child);
                }
            }
        }
        if (!file.delete()) {
            System.out.println("Warning: unable to delete " + file);
        }
    }


    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, File jarFile) throws ImplerException {
        implement(token, new File("tmp"));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("Error: compiler not found");
            throw new ImplerException();
        }
        int exitCode = compiler.run(null, null, null, targetPath.toString());
        if (exitCode != 0) {
            System.err.println("Error: compiler exits with errors");
            throw new ImplerException();
        }

        try (FileOutputStream out = new FileOutputStream(jarFile);
             JarOutputStream jarOut = new JarOutputStream(out);
             InputStream in = new BufferedInputStream(
                     new FileInputStream("tmp" + File.separator + targetClassPath))) {
            jarOut.putNextEntry(new ZipEntry(targetClassPath.toString()));
            int bytesRead;
            byte[] buffer = new byte[8 * 1024];

            while ((bytesRead = in.read(buffer)) != -1) {
                jarOut.write(buffer, 0, bytesRead);
            }
            jarOut.closeEntry();
            clean(new File("tmp"));
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
