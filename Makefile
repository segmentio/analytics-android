#
# Targets.
#

clean:
	@./gradlew clean

test:
	@./gradlew check test

build:
	@./gradlew build

#
# Phonies.
#

.PHONY: clean
.PHONY: test
.PHONY: build

