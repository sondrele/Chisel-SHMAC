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

.PHONY: clean all router units tiles verilog

all:
	$(SBT) "$(RUN) $@ $(TARGET) $(SW) $(TEST)"

router:
	$(SBT) "$(RUN) $@ $(TARGET) $(SW) $(TEST)"

units:
	$(SBT) "$(RUN) $@ $(TARGET) $(SW) $(TEST)"

tiles:
	$(SBT) "$(RUN) $@ $(TARGET) $(SW) $(TEST)"

%.out:
	$(SBT) "$(RUN) $(@:.out=) $(TARGET) $(SW) $(TEST)"

verilog:
	$(SBT) "run $@ $(HW)"

clean:
	$(SBT) clean
	rm -rf $(TARGET_DIR)
