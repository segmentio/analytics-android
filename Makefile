#
# Targets.
#

clean:
	@./gradlew clean

test:
	@./gradlew test

build:
	@./gradlew build

#
# Phonies.
#

.PHONY: clean
.PHONY: test
.PHONY: build

