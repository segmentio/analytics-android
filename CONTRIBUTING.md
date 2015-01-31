
# Contributing

We're huge fans of open-source, and absolutely we love getting good contributions to **analytics-android**! These docs will tell you everything you need to know about how to add your own integration to the library with a pull request, so we can merge it in for everyone else to use.

  1. Apply to be a Segment partner: https://segment.com/docs/partners/join-the-platform/
  2. Complete a Technical Survey: https://segment.com/partners/techqs
  3. Hear from the Segment team with the details for submitting your pull request.
  
## Getting Setup

To start, we'll get you set up with a development environment. You'll need the [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed on your computer. If you're on a windows computer, just replace  `./gradlew` with `gradlew.bat` for all the Gradle commands below.

Then after forking **analytics-android** just `cd` into the folder and run `./gradlew clean build`:

    $ cd analytics-android
    $ ./gradlew clean build

That will download the required version of Gradle, setup the Android SDK if needed, install all our maven dependencies, run [checkstyle](http://checkstyle.sourceforge.net) and unit tests. If you have an existing Android installation, you should export a variable `$ANDROID_HOME` pointing to the location of the SDK. You can now add your changes to the library, and run `./gradlew test` to make sure everything is passing still.

The commands you'll want to know for development are:

    $ ./gradlew assemble # re-compiles the development build of analytics-android for testing
    $ ./gradlew test     # runs all of the tests
    $ ./gradlew check    # runs all verification tasks, such as lint, checkstyle and tests
    $ ./gradlew clean    # clears the build output folder

You can also run `./gradlew tasks --all` to see a complete list of available commands.

    $ ./gradlew tasks --all


## Getting Setup

  To start contributing integrations, you may want to use a couple of tools that will help you integrate as fast as possible:

  - [Khaos](https://github.com/segmentio/khaos) (`npm i -g khaos`)
  - [khaos-android-integration](https://github.com/segmentio/khaos-android-integration) (`khaos install segmentio/khaos-android-integration analytics-android-integration`)

Once you have those tools installed, `cd` into your fork and run:

  ```bash
  $ khaos create analytics-android-integration <slug>
  ```

Khaos will ask you a couple of question and create the integration skeleton for you!
See our [tracking API](https://segment.com/docs/tracking-api/) to check what each method does. You should copy the generate files into the `src` and `test` folders.


## Adding an Integration

We've written **analytics-android** to be very modular, so that adding new integrations is incredibly easy. Once you've generated the templates with khaos (from the step above). The `IntegrationManager` has a factory method `createIntegrationForKey` that uses a trie to match keys to the integration constructor.

```java
  AbstractIntegration createIntegrationForKey(String key) {
    switch (key.charAt(0)) {
      case 'A':
        switch (key.charAt(1)) {
          case 'm':
            verify(key, AmplitudeIntegration.AMPLITUDE_KEY);
            return new AmplitudeIntegration();
          case 'p':
            verify(key, AppsFlyerIntegration.APPS_FLYER_KEY);
            return new AppsFlyerIntegration();
          default:
            break;
        }
      case 'B':
        verify(key, BugsnagIntegration.BUGSNAG_KEY);
        return new BugsnagIntegration();
      ...
      default:
        break;
    }
    // this will only be called for bundled integrations, so should fail if we see some unknown
    // bundled integration!
    throw new AssertionError("unknown integration key: " + key);
  }
```

Add a line similar to `findBundledIntegration("com.amplitude.api.Amplitude", AmplitudeIntegration.AMPLITUDE_KEY);` in the `IntegrationManager` constructor, that will look for the appropriate class for the integration. This is done so that every event can disable which events should be sent through our servers, and which events should be sent through the bundled integration.

To get a good idea of how an integration works, check out some of our [existing](https://github.com/segmentio/analytics-android/blob/master/core/src/main/java/com/segment/analytics/LocalyticsIntegration.java) [integration](https://github.com/segmentio/analytics-android/blob/master/core/src/main/java/com/segment/analytics/LeanplumIntegration.java) [files](https://github.com/segmentio/analytics-android/blob/master/core/src/main/java/com/segment/analytics/BugsnagIntegration.java).

_Note: if you wanted to add your own private integration, you'd do exactly the same thing, just inside your own codebase! But public ones are way more awesome because everyone gets to share them and improve them..._


## Writing Tests

Every contribution you make to **analytics-android** should be accompanied by matching tests. If you look inside of the `*Test.java` file of any integration, you'll see we're pretty serious about this. That's because:

2. **analytics-android** runs on tons of different versions of Android and we want to make sure it runs well everywhere without having to manually test each integration.
3. It lets us add new features much, much more quickly.
1. We aren't insane.

When adding your own integration, the easiest way to figure out what major things to test is to look at everything you've added to the integration. You'll want to write testing groups for `#initialize`, `#load`, `#identify`, `#track`, etc. And each group should test all of the expected functionality.

The most important thing to writing clean, easy-to-manage integrations is to **keep them small** and **clean up after each test**, so that the environment is never polluted by an individual test.
If you run into any questions, check out a few of our [existing test files](https://github.com/segmentio/analytics-android/tree/master/core/src/androidTest/java/com/segment/analytics) to see how we've done it.

## Contributing Checklist

To help make contributing easy, here's all the things you need to remember:

- Generate a template, either from your IDE or using Khaos
- Add methods you want to support to the `prototype`. (`identify`, `track`, `pageview`, etc.)
- Write tests for all of your integration's logic.
- Add your integration file to `src` and the test file to `androidTest`.
- Run the tests and get everything passing.
- Commit your changes with a nice commit message.
- Submit your pull request.
