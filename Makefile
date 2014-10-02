# The source directory is configured in build.sbt
# The first two arguments are consumed by sbt, the rest is
# forwarded to the Scala/Chisel main

SBT ?= sbt
# package.Object (main)
MAIN = router.Main
TARGET_DIR = generated

TARGET = --targetDir $(TARGET_DIR)
SW = --backend c
HW = --backend v
TEST = --compile --genHarness --test

.PHONY: clean

test:
	$(SBT) "run-main $(MAIN) $(TARGET) $(SW) $(TEST)"

verilog:
	$(SBT) "run-main $(MAIN) $(TARGET) $(HW)"

clean:
	$(SBT) clean
	rm -rf $(TARGET_DIR)
