package de.phib.jira;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

/**
 * Provides the functionality to execute a search for issues in JIRA based on a given JQL search query and returns the
 * sum of the remaining estimates of the resulting issues.
 *
 * @author Philippe Boessling
 */
public class JiraEstimatesCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(JiraEstimatesCalculator.class);

    private static final int SEARCH_MAX_RESULTS = 10_000;

    private SearchRestClient searchClient;

    /**
     * Creates a new instance of JiraEstimatesCalculator.
     *
     * @param uri the URI of JIRA
     * @param username the name of the user to access the JIRA REST API
     * @param password the password of the user
     */
    public JiraEstimatesCalculator(String uri, String username, String password) {
        try {
            final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            final URI jiraUri = new URI(uri);
            final JiraRestClient jiraRestClient = factory.createWithBasicHttpAuthentication(jiraUri, username, password);
            this.searchClient = jiraRestClient.getSearchClient();
        } catch(URISyntaxException e) {
            LOG.error("An error occurred while creating the JiraRestClient. The syntax of given URL '" + uri + "' is invalid.", e);
        }

    }

    /**
     * Executes a search for issues in JIRA based on a given JQL search query and returns the sum of the remaining
     * estimates of the resulting issues.
     *
     * @param jql a JQL search query
     * @return the sum of the remaining estimates of the resulting issues
     */
    public int calculateRemainingEstimates(String jql) {
        Iterable<Issue> issues = searchIssues(jql);

        int estimates = 0;
        for (Issue issue : issues) {
            estimates += getRemainingEstimateFromField(issue);
        }

        LOG.info("Remaining estimates (in seconds): " + estimates);
        LOG.info("Remaining estimates (in working days): " + ((double) estimates) / (60 * 60 * 8));

        return estimates;
    }

    /**
     * Executes a search for issues in JIRA based on a given JQL search query and returns the resulting issues.
     *
     * For more information about the JIRA REST API for search see:
     * https://docs.atlassian.com/jira-rest-java-client-parent/5.0.4/apidocs/com/atlassian/jira/rest/client/api/SearchRestClient.html#searchJql(java.lang.String,%20java.lang.Integer,%20java.lang.Integer,%20java.util.Set)
     *
     * @param jql a JQL search query
     * @return the resulting issues
     */
    private Iterable<Issue> searchIssues(String jql) {
        Iterable<Issue> issues = Iterables.emptyIterable();

        try {
            LOG.info("Starting search...");
            LOG.info("Search query: " + jql);

            Promise<SearchResult> result = this.searchClient.searchJql(jql, SEARCH_MAX_RESULTS, 0, null);
            SearchResult resultObject = result.get();
            issues = resultObject.getIssues();

            LOG.info("Search finished.");
            LOG.info("Number of results: " + resultObject.getTotal());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("An error occurred during the search for issues with the query '" + jql + "'.", e);
        }

        return issues;
    }

    /**
     * Retrieves the value of the field containing the remaining estimate of a given issue
     *
     * @param issue an issue
     * @return the value of the field containing the remaining estimate of the issue
     */
    private int getRemainingEstimateFromField(Issue issue) {
        IssueField estimateField = issue.getField("timeestimate");

        if(estimateField != null) {
            Integer estimateValue = (Integer) estimateField.getValue();

            if (estimateValue != null) {
                return estimateValue;
            }
        }

        return 0;
    }

}