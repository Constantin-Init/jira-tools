package de.phib.jiratools.tools;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality to return the sum of the remaining estimates of a given list of issues.
 */
public class CalculateRemainingEstimates {

    private static final Logger LOG = LoggerFactory.getLogger(CalculateRemainingEstimates.class);

    private CalculateRemainingEstimates() {
        // no op
    }

    /**
     * Returns the sum of the remaining estimates of the given issues.
     *
     * @param issues a list of issues
     * @return the sum of the remaining estimates of the issues
     */
    public static int getRemainingEstimates(Iterable<Issue> issues) {

        int estimates = 0;
        for (Issue issue : issues) {
            estimates += getRemainingEstimateFromField(issue);
        }

        LOG.info("Remaining estimates (in seconds): {}", estimates);
        LOG.info("Remaining estimates (in working days): {}", ((double) estimates) / (60 * 60 * 8));

        return estimates;
    }

    /**
     * Retrieves the value of the field containing the remaining estimate of a given issue
     *
     * @param issue an issue
     * @return the value of the field containing the remaining estimate of the issue
     */
    private static int getRemainingEstimateFromField(Issue issue) {
        IssueField estimateField = issue.getField("timeestimate");

        if (estimateField != null) {
            Integer estimateValue = (Integer) estimateField.getValue();

            if (estimateValue != null) {
                return estimateValue;
            }
        }

        return 0;
    }


}