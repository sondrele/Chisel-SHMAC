# The source directory is configured in build.sbt
# The first two arguments are consumed by sbt, the rest is
# forwarded to the Scala/Chisel main

SBT ?= sbt
# package.Object (main)
MAIN = TestMain
RUN = run-main $(MAIN)
TARGET_DIR = generated

TARGET = --targetDir $(TARGET_DIR)
SW = --backend c
HW = --backend v
TEST = --compile --genHarness --test

.PHONY: clean testall verilog

testall:
	$(SBT) "$(RUN) $@ $(TARGET) $(SW) $(TEST)"

%.out:
	$(SBT) "$(RUN) $(@:.out=) $(TARGET) $(SW) $(TEST)"

verilog:
	$(SBT) "$(RUN) $(TARGET) $(HW)"

clean:
	$(SBT) clean
	rm -rf $(TARGET_DIR)
