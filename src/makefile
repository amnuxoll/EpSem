JC = javac
JARGS = -Xlint:unchecked
CP = ".:../dependencies/*"
JVM = java

.SUFFIXES:
.SUFFIXES: .java .class

FRAMEWORK = framework/*.java

AGENTS = agents/**/*.java

ENVIRONMENTS = environments/**/*.java

EXPERIMENTS = experiments/*.java

TESTS = tests/**/*.java

print-% : ; @echo $* = $($*)

default: all 

frameworkTGT:  
	$(JC) $(JARGS) $(FRAMEWORK)

agentsTGT: frameworkTGT
	$(JC) $(JARGS) $(AGENTS)

environmentsTGT: frameworkTGT
	$(JC) $(JARGS) $(ENVIRONMENTS)

experimentsTGT: agentsTGT environmentsTGT
	$(JC) $(JARGS) $(EXPERIMENTS)

testsTGT: experimentsTGT
	$(JC) $(JARGS) -cp $(CP) $(TESTS)

all: frameworkTGT agentsTGT environmentsTGT experimentsTGT testsTGT
	
runexperiments: all
	$(JVM) experiments.Runner

runtests: all
	$(JVM) -jar ../dependencies/junit-platform-console-standalone-1.3.0-RC1.jar -d tests/framework/ --class-path $(CP) -e junit-jupiter	

clean:
	$(RM) **/*.class
