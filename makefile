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
	$(JC) $(JARGS) $(ALLSRC)

runexperiments: all
	$(JVM) -cp $(CP) experiments.Runner

runtests: all
	$(JVM) -cp $(call classpathify,$(OUTDIRS)) tests.EpSemTestRunner out/tests

runtestsnorebuild:
	$(JVM) -cp $(call classpathify,$(OUTDIRS)) tests.EpSemTestRunner out/tests

clean:
	$(RM) -rf ../out/* 