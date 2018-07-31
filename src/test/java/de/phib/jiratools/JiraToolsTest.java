package de.phib.jiratools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the class JiraTools.
 */
public class JiraToolsTest {

    private static JiraTools jiraTools;

    /**
     * Creates an instance of JiraTools to be used in the unit tests.
     */
    @BeforeAll
    public static void setupTests() {
        jiraTools = new JiraTools("http://example.com", "jirauser", "secret");
    }

    /**
     * Tests the method CalculateRemainingEstimates#getRemainingEstimates(java.lang.String).
     */
    @Test
    public void testCalculateRemainingEstimates() {
        int estimates = jiraTools.calculateRemainingEstimates("project = DEMO");

        Assertions.assertTrue(estimates > -1);
    }

    /**
     * Tests the method CalculateRemainingEstimates#getRemainingEstimates(java.lang.String).
     */
    @Test
    public void testGenerateReleaseNotes() {
        String releaseNotes = jiraTools.generateReleaseNotes("project = DEMO AND fixVersion = 1.0 AND level = \"public\" ORDER BY issuetype DESC, key ASC");

        Assertions.assertTrue(releaseNotes != null);
    }

}
