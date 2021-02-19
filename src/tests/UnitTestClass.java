package tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class UnitTestClass {
    //region Class Variables
    private Class testClass;
    private Object testClassInstance;
    private List<Method> unitTests;
    //endregion

    //region Constructors
    @SuppressWarnings("unchecked")
    public UnitTestClass(Class testClass, List<String> methods) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        this.testClass = testClass;
        this.testClassInstance = this.testClass.getConstructor().newInstance();
        this.unitTests = Arrays.stream(this.testClass.getMethods())
                                .filter(method -> method.getAnnotation(EpSemTest.class) != null && (methods.contains(method.getName())))
                                .collect(Collectors.toList());
    }
    //endregion

    //region Public Methods
    public String getClassName() { return this.testClass.getName(); }

    public Results executeTestClass() {
        System.out.println("Running test class: " + this.testClass.getName());
        Results results = new Results(this.testClass.getName());
        for (Method test : this.unitTests) {
            try {
                test.invoke(this.testClassInstance);
                results.addResult(test.getName());
            }catch (InvocationTargetException exc) {
                // This would be our failed assertions (most of the time... hopefully)
                results.addResult(test.getName(), (Exception) exc.getTargetException());
            } catch (Exception exception) {
                results.addResult(test.getName(), exception);
            }
        }
        return results;
    }
    //endregion




}
