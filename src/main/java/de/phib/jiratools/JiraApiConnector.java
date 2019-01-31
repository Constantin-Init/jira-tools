package de.phib.jiratools;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

/**
 * Provides the functionality to execute a search for issues in JIRA based on a given JQL search query
 */
public class JiraApiConnector {

    private static final Logger LOG = LoggerFactory.getLogger(JiraApiConnector.class);

    private static final int SEARCH_MAX_RESULTS = 10000;

    private JiraRestClient jiraRestClient;

    /**
     * Creates a new instance of JiraApiConnector.
     *
     * @param uri      the URI of JIRA
     * @param username the name of the user to access the JIRA REST API
     * @param password the password of the user
     */
    public JiraApiConnector(String uri, String username, String password) {
        try {
            AsynchronousJiraRestClientFactory clientFactory = new AsynchronousJiraRestClientFactory();
            URI jiraUri = new URI(uri);
            this.jiraRestClient = clientFactory.createWithBasicHttpAuthentication(jiraUri, username, password);
        } catch (URISyntaxException e) {
            LOG.error("An error occurred while creating the JiraRestClient. The syntax of given URL '" + uri + "' is invalid.", e);
        }
    }

    /**
     * Executes a search for issues in JIRA based on a given JQL search query and returns the resulting issues.
     * <p>
     * For more information about the JIRA REST API for search see:
     * https://docs.atlassian.com/jira-rest-java-client-parent/5.0.4/apidocs/com/atlassian/jira/rest/client/api/SearchRestClient.html#searchJql(java.lang.String,%20java.lang.Integer,%20java.lang.Integer,%20java.util.Set)
     *
     * @param jql a JQL search query
     * @return the resulting issues
     */
    public Iterable<Issue> searchIssues(String jql) {
        Iterable<Issue> issues = Iterables.emptyIterable();

        try {
            LOG.debug("Starting search...");
            LOG.debug("Search query: {}", jql);

            Promise<SearchResult> result = getSearchClient().searchJql(jql, SEARCH_MAX_RESULTS, 0, null);
            SearchResult resultObject = result.get();
            issues = resultObject.getIssues();

            LOG.debug("Search finished.");
            LOG.debug("Number of results: {}", resultObject.getTotal());
        } catch (InterruptedException e) {
            LOG.error("An error occurred during the search for issues with the query '" + jql + "'.", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("An error occurred during the search for issues with the query '" + jql + "'.", e);
        }

        return issues;
    }

    private SearchRestClient getSearchClient() {
        return jiraRestClient.getSearchClient();
    }
}
