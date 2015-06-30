
# Contributing

We're huge fans of open-source, and absolutely we love getting good contributions to **analytics-android**! These docs will tell you everything you need to know about how to add your own integration to the library with a pull request, so we can merge it in for everyone else to use.

  1. Apply to be a Segment partner: https://segment.com/docs/partners/join-the-platform/
  2. Complete a Technical Survey: https://segment.com/partners/techqs
  3. Hear from the Segment team with the details for submitting your pull request.
  
## Getting Setup

To start, we'll get you set up with a development environment. You'll need the [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed on your computer. Note that you'll need JDK 8 to run all the tests. You'll also need the Android SDK installed. If you have an existing Android installation, make sure you export a variable `$ANDROID_HOME` pointing to the location of the SDK. If you're on a windows computer, just replace  `./gradlew` with `gradlew.bat` for all the Gradle commands below.

Then after forking **analytics-android**, `cd` into the folder and run `./gradlew clean build`:

    $ cd analytics-android
    $ ./gradlew clean build

This will download the required version of Gradle, install all dependencies, run [checkstyle](http://checkstyle.sourceforge.net) and unit tests. You can now add your changes to the library, and run `./gradlew test` to make sure everything is still working.

The commands you'll want to know for development are:

    $ ./gradlew assemble # re-compiles the development build of analytics-android for testing
    $ ./gradlew test     # runs all of the tests
    $ ./gradlew check    # runs all verification tasks, such as lint, checkstyle and tests
    $ ./gradlew clean    # clears the build output folder

You can also run `./gradlew tasks --all` to see a complete list of available commands.

    $ ./gradlew tasks --all


## Adding an Integration

We've written **analytics-android** to be very modular, so that adding new integrations is incredibly easy.

First, you need to write your integration. You can copy the template in the `contributing` folder to the `analytics-integrations` directory and replace references to `mytool` with your own integration and SDK.

Then you'll need to update our SDKs to know about the new integration.

1. Update `Analytics.BundledIntegration` in [`analytics-core/src/main/java/com/segment/analytics/Analytics.java`](https://github.com/segmentio/analytics-android/blob/master/analytics-core/src/main/java/com/segment/analytics/Analytics.java) and add `MYTOOL("MyTool")` as an BundledIntegration constant.
2. Update [`IntegrationManager`](https://github.com/segmentio/analytics-android/blob/master/analytics-core/src/main/java/com/segment/analytics/IntegrationManager.java) and add `loadIntegration("com.segment.analytics.internal.integrations.MyToolIntegration");` to the `loadIntegrations()` method.
3. Update [`settings.gradle`](https://github.com/segmentio/analytics-android/blob/master/settings.gradle) to pick up your integration.
4. Update [our master project](https://github.com/segmentio/analytics-android/blob/master/analytics/build.gradle) and add your integration as a dependency.

To get a good idea of how an integration works, check out some of our [existing integrations](https://github.com/segmentio/analytics-android/tree/master/analytics-integrations).

_Note: if you wanted to add your own private integration, you'd do exactly the same thing, just inside your own codebase! But public ones are way more awesome because everyone gets to share them and improve them..._


## Writing Tests

Every contribution you make to **analytics-android** should be accompanied by matching tests. If you look inside of the `*Test.java` file of any integration, you'll see we're pretty serious about this. That's because:

2. **analytics-android** runs on tons of different versions of Android and we want to make sure it runs well everywhere without having to manually test each integration.
3. It lets us add new features much, much more quickly.
1. We aren't insane.

When adding your own integration, the easiest way to figure out what major things to test is to look at everything you've added to the integration. You'll want to write testing groups for `#initialize`, `#load`, `#identify`, `#track`, etc. And each group should test all of the expected functionality.

The most important thing to writing clean, easy-to-manage integrations is to **keep them small** and **clean up after each test**, so that the environment is never polluted by an individual test.
If you run into any questions, check out a few of our existing test files to see how we've done it.

## Contributing Checklist

To help make contributing easy, here's all the things you need to remember:

- Create your integration, either from your IDE or using our template
- Add methods you want to support to the `prototype`. (`identify`, `track`, `screen`, etc.)
- Write tests for all of your integration's logic.
- Run the tests and get everything passing.
- Commit your changes with a nice commit message.
- Submit your pull request.
