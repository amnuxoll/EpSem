package tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpSemTestRunner {
    //region Main
    public static void main(String[] args) {
        File file;
        if (args == null || args.length == 0) {
            File srcDir = new File(System.getProperty("user.dir"), "src");
            file = new File(srcDir, "tests");
        }
        else {
            file = new File(args[0]);
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ArrayList<UnitTestClass> testClasses = getUnitTestClasses(classLoader, file);
        ArrayList<Results> results = new ArrayList<>();
        for (UnitTestClass testClass : testClasses) {
	        try {
                results.add(testClass.executeTestClass());
	        } catch (Exception ex) {
                ex.printStackTrace();
	        }
        }
        int successCount = 0, failureCount = 0;
        for (Results result : results) {
            result.print(false);
            successCount += result.getSuccessCount();
            failureCount += result.getFailureCount();
        }
        System.out.println();
        System.out.println("Total tests executed: " + (successCount + failureCount));
        System.out.println("Success Count: " + successCount);
        System.out.println("Failure Count: " + failureCount);
    }
    //endregion

    //region Private Static Methods
    @SuppressWarnings("unchecked")  //TODO:  can we resolve this more cleanly?
    private static ArrayList<UnitTestClass> getUnitTestClasses(ClassLoader classLoader, File directory) {
        ArrayList<UnitTestClass> files = new ArrayList<>();
        if (directory.isFile()) {
            try {
                String classPath = toClassPath(directory);
                Class testClass = classLoader.loadClass(classPath);
                if (testClass.getAnnotation(EpSemTestClass.class) != null)
                    files.add(new UnitTestClass(testClass));
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            for (File file : directory.listFiles()) {
                files.addAll(getUnitTestClasses(classLoader, file));
            }
        }
        return files;
    }

    private static String toClassPath(File file) {
        StringBuilder classPath = new StringBuilder(file.getName().replace(".java", "").replace(".class",""));
        File currentFile = file.getParentFile();
        while (currentFile != null && !(currentFile.getName().equals("src") || currentFile.getName().equals("out"))) {
            classPath.insert(0, currentFile.getName() + ".");
            currentFile = currentFile.getParentFile();
        }
        return classPath.toString();
    }
    //endregion
}
