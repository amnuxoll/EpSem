How to use this framework
Author: Zachary Paul Faltersack
Version: 0.95

This framework has the intention of:
 1. Enabling the development of well-tested agents with reusable components;
 2. Easing the process of gathering data by creating well-defined interfaces such that environments and agents can
    easily be mixed-and-matched.

When developing against the framework, try not to modify anything in the framework package. As this is an evolving
project it may be the case that you need to add support for something that is not yet supported, but this should be
rare at this point. Instead, use utils/agents/environments for building out your project.
An example where modification might be necessary is adding new introspection capabilities (see Introspection below).

Important: Be sure to regularly run unit tests. If changes you made break any tests, be sure to dig into why, especially
for shared utility classes.


: AGENTS
The following sections describe the techniques for creating new agents in the framework.

:: Writing an Agent
 1. Create package in: root/agents/X
 2. Make a new agent class that implements: framework.IAgent
 3. While not required, it is recommended to use the utility class utils.EpisodicMemory, as it contains many commonly
    used operations against a series of episodes.
 4. Make a new agent provider class that implements: framework.IAgentProvider which will create new instances of your
    agent.

When developing your agent, consider exactly what you are trying to accomplish at any given moment. If it makes sense,
create a reusable and well-tested data structure or utility class for the logic you are writing (for example
EpisodicMemory which abstracts away common operations from the consumer).

:: Getting data from an Agent
There are several integration points that allow for data gathering in an agent. These are all optional (defaulted)
methods on IAgent.

::: For gathering statistical data that will be written to a CSV file do the following:
 1. Override both IAgent.getStatisticTypes() and IAgent.getData(). The current version of the framework requires
    that you return the union of your statistic types and IAgent.super.getStatisticTypes(), as the interface default
    implementation contains expected framework-level elements.
 2. getStatisticTypes(): This will return an array of strings with the names of the statistics your agent aims to gather.
    This is a contract with the test framework that promises to return a value for each named statistic when getData()
    is invoked.
 3. getData(): This will return a list of data points, one for each statistic named in getStatisticTypes(). It is
    the onus of the developer to respect this contract.

If implemented correctly then the output directory will contain a CSV file for each declared statistic type, and each
data point will be that data point at the time that particular goal index was hit.

::: Additional logging options are:
 1. Override onGoalFound(): When the framework detects a goal is hit, it will invoke this method with no arguments. It
    is a point for the agent to perform non-statistical logging if desired.
 2. Override onTestRunComplete(): When the framework completes a test run (This is defined as having located the goal
    a specific number of times in a given environment) then it will invoke this method with no arguments and is another
    opportunity to log non-statistical data.

An example of something that could be useful here is to print the internal state of the agent after each goal and after
the test run is complete.

::: Introspection
It may be the case that the agent wants to track some data about itself that requires knowledge of the environment that
is otherwise inappropriate for it to know. To support this, when the agent is initialized it is given an IIntrospector.
This is an interface that allows the agent to ask for specific information, such as whether or not the sequence it has
selected will work if it were to run through to completion.

It is important not to leverage this for anything other than statistics and logging. No information received via
introspection should be considered viable for learning and/or making decisions about what to do next; it is a high-level
analytics tool for developers to use for gathering details about how the agent is working and the decisions it's making.


: LOGGING
The framework provides a singleton class called NamedOutput that is for logging. When writing a logging statement to
this object, a name/tag is provided that can be used to group specific logging statements. In a default instance, the
logging will go to System.out. It is possible, however, to redirect specific named output using the instance method
NamedOutput.configure(...) which takes a name and an OutputStream. If a name has been configured then it will route
the logging statements to whichever stream it was given (for example into a file for persistence). See TestSuite (below)
for details on how this routing can be leveraged.


: ENVIRONMENTS
The following sections describe the techniques for creating new environments in the framework. Environments are defined
with a description that is leveraged by the framework Environment class.

:: Writing an Environment
 1. Create package in: root/environments/X
 2. Make a new environment description class that implements: framework.IEnvironmentDescription
 3. Make a new environment description provider class that implements: framework.IEnvironmentDescriptionProvider
    which will create new instances of your environment description.

When developing your environment description, consider exactly what you are trying to accomplish at any given moment.
If it makes sense, create a reusable and well-tested data structure or utility class for the logic you are writing
(for example FSMTransitionTable which encapsulates analysis of a set of FSM transitions).


: TESTRUN
A TestRun is simply one agent and one environment with a specific number of goals for the agent to find. The TestRun
class will, when executed, marshal calls between the agent and the environment, track the number of steps the
agent has taken between successive goals, and inform the TestSuite when a goal is hit so that the statistical data
can be written to a result writer.


: TESTSUITE
A test suite is a collection of environments and agents, along with a configuration that indicates details like how
many goals should be found, etc. It will create a TestRun for each element in the cross product of the sets of agents
and environments.
Before any tests are run, however, it will invoke a delegate that can be optionally passed into the constructor. This
allows a developer to define some configurations (such as routing NamedOutput) before any tests are run. The delegate
receives a single argument that is a File which designates the target output directory for all data files.

A constructor can be used as follows for hooking into the pre-run delegate:

    private static TestSuite TempExperiment = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] { ... },
            new IAgentProvider[] { ... }
    );

An instance of an agent is created with the agent providers and an instance of an environment description is created
with the environment description providers. These are then put through a TestRun based on the given
TestSuiteConfiguration.

: UNIT TESTING
This framework has homegrown unit test functionality that is currently evolving. The intention is to minimize as much
as possible any external dependencies such as JUnit. In order to create a new test class you must annotate the class
as well as any test methods in the class as follows:

@EpSemTestClass
public class ThisIsATestClass {
    @EpSemTest
    public void ThisIsATest() {
        // I must not take any arguments.
    }
}

When writing tests, nest them in the "tests" package in a directory
structure that reflects the source code structure of the rest of the
project.  For example, if you have just created:
   src/agents/foo/FooAgent.java
and you want to write a test for it you would create this file:
   src/tests/agents/foo/FooAgentTest.java
    

tests.Assertions contains a variety of static assertion methods. Use these and if an assertion
does not exist for what you need, add it here and ensure it throws the correct AssertionFailedException.

There is a test runner class called EpSemTestRunner that takes a single argument. If this argument is a directory, then
that directory will be searched recursively for test classes. If it is a single file, then it will run that test file.
For example:

java <classpath> tests.EpSemTestRunner tests/mytestfile.java
java <classpath> tests.EpSemTestRunner tests/

: MAKE
A makefile exists in the repository root that contains a variety of targets.

make [all] :: This will build all the source code and put the generated class files in <root>/out/

make runexperiments :: This will build the source code and then execute whatever experiment is in Runner.main().

make runtests :: This will build the source code and then run all the unit tests

make runtestsnorebuild :: This will run all the unit tests without rebuilding the source code.

This functionality is new and still brittle. It is expected that all commands to make are executed from the root
repository directory for now. This will continue to evolve and become more robust.
