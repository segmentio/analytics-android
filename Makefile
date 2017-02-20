#
# Targets.
#

clean:
	@./gradlew clean

test:
	@./gradlew check test

build:
	@./gradlew build

JAVA_FILES = $(shell find . -type f -name "*.java" | grep ".*/src/.*java")

fmt:
	@java -jar codestyle/google-java-format-1.3-all-deps.jar -i $(JAVA_FILES)

#
# Phonies.
#

.PHONY: clean
.PHONY: test
.PHONY: build
.PHONY: fmt
