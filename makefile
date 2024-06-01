JC = javac
JARGS = -Xlint:unchecked -d out
JVM = java

.SUFFIXES:
.SUFFIXES: .java .class

SRCDIR := src
OUTDIR := out

ALLSRC := $(shell find $(SRCDIR) -name '*.java')
OUTDIRS := $(shell find $(OUTDIR) -type d)

print-% : ; @echo $* = $($*)
classpathify = $(subst $(eval) ,:,$(wildcard $1))

default: all 

all:
	# create the 'out' subfolder if it doesn't exist
	[ -d out ] && echo "" || mkdir out
	@$(JC) $(JARGS) $(ALLSRC)

runexperiments: all
	$(JVM) -cp $(call classpathify,$(OUTDIRS)) experiments.Runner

runtests: all
	$(JVM) -cp $(call classpathify,$(OUTDIRS)) tests.EpSemTestRunner out/tests

runtestsnorebuild:
	$(JVM) -cp $(call classpathify,$(OUTDIRS)) tests.EpSemTestRunner out/tests

clean:
	$(shell rm `find $(OUTDIR) -name "*.class"`)
