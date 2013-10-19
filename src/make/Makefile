JAVAC=javac
JAR=jar
CD=cd
RM=rm
MKDIR=mkdir
SRC=../classes
BUILD=../../lib
CP=../../lib/commons-math-1.1.jar

default compare.jar:
	$(RM) -rf $(BUILD)/compare.jar
	$(JAVAC) -cp $(CP) -d $(BUILD) -sourcepath $(SRC) $(SRC)/org/bzewdu/*/*.java
	$(CD) $(BUILD) ; $(JAR) cfm compare.jar ../src/make/Manifest *
