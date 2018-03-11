package de.phib.jira;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the class JiraEstimatesCalculator.
 */
public class JiraEstimatesCalculatorTest {

    private static JiraEstimatesCalculator jiraEstimatesCalculator;

    /**
     * Creates an instance of JiraEstimatesCalculator to be used in the unit tests.
     */
    @BeforeAll
    public static void setupTests() {
        jiraEstimatesCalculator = new JiraEstimatesCalculator("http://example.com", "jirauser", "secret");
    }

    /**
     * Tests the method JiraEstimatesCalculator#calculateRemainingEstimates(java.lang.String).
     */
    @Test
    public void testCalculateRemainingEstimates() {
        int estimates = jiraEstimatesCalculator.calculateRemainingEstimates("project = DEMO");

        Assertions.assertTrue(estimates > -1);
    }

}