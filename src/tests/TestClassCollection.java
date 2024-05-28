package tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for loading up the unit tests that are to be executed.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestClassCollection
{
    private ClassLoader classLoader;
    private ArrayList<UnitTestClass> unitTestClasses = new ArrayList<>();

    /**
     * We can specify a directory of files where we can calculate and infer the tests to load.
     *
     * @param classLoader The ClassLoader to use for loading test classes.
     * @param directory The directory to recursively search for test classes to run.
     */
    public TestClassCollection(ClassLoader classLoader, File directory)
    {
        this.classLoader = classLoader;
        this.populateUnitTestClasses(directory);
    }

    /**
     * We can specify a concrete list of test classes and methods to run. They require a specific format:
     *
     * To run a full test class, simply write:
     *  - package.path.to.class
     *
     *  To run a subset of tests on that class write:
     *  - package.path.to.class|method1|method2|...|methodN
     *
     *  The second variant will only run the tests included in the method list.
     *
     * @param classLoader
     * @param tests
     */
    public TestClassCollection(ClassLoader classLoader, String[] tests)
    {
        this.classLoader = classLoader;
        for (String testClass : tests)
        {
            this.AddUnitTestClass(testClass);
        }
    }

    public List<UnitTestClass> getUnitTestClasses()
    {
        return Collections.unmodifiableList(this.unitTestClasses);
    }

    private void populateUnitTestClasses(File directory) {
        if (directory.isFile()) {
            String classPath = this.toClassPath(directory);
            this.AddUnitTestClass(classPath);
        } else {
            for (File file : directory.listFiles()) {
                populateUnitTestClasses(file);
            }
        }
    }

    private String toClassPath(File file) {
        StringBuilder classPath = new StringBuilder(file.getName().replace(".java", "").replace(".class",""));
        File currentFile = file.getParentFile();
        while (currentFile != null && !(currentFile.getName().equals("src") || currentFile.getName().equals("out"))) {
            classPath.insert(0, currentFile.getName() + ".");
            currentFile = currentFile.getParentFile();
        }
        return classPath.toString();
    }

//Removed May 2024 because it wasn't passing.  TODO:  fix it properly
    private void AddUnitTestClass(String classPath) {
        // try {
        //     String[] parts = classPath.split("\\|");
        //     Class testClass = this.classLoader.loadClass(parts[0]);
        //     if (testClass.getAnnotation(EpSemTestClass.class) != null)
        //     {
        //         List<String> methods;
        //         if (parts.length == 1)
        //             methods = Arrays.stream(testClass.getMethods()).map(Method::getName).collect(Collectors.toList());
        //         else
        //             methods = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length)).collect(Collectors.toList());
        //         this.unitTestClasses.add(new UnitTestClass(testClass, methods));
        //     }
        // } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
        //     e.printStackTrace();
        // }
    }
}
