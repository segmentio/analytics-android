package com.segment.analytics;

import java.util.Arrays;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static junit.framework.Assert.fail;

/**
 * A {@link TestRule} which should be used for testing implementations of AbstractIntegration. This
 * will verify that the test class has methods for all methods that an integration can implement.
 */
public class IntegrationTestRule implements TestRule {
  List<String> expectedMethodNames =
      Arrays.asList("initialize", "activityCreate", "activityStart", "activityResume",
          "activityPause", "activityStop", "activitySaveInstance", "activityDestroy", "identify",
          "group", "track", "alias", "screen", "flush", "reset");

  @Override public Statement apply(Statement base, Description description) {
    Class<?> testClass = description.getTestClass();

    for (String methodName : expectedMethodNames) {
      try {
        testClass.getMethod(methodName);
      } catch (NoSuchMethodException e) {
        fail(testClass + " did not have a test for method: " + methodName);
      }
    }

    return base;
  }
}
