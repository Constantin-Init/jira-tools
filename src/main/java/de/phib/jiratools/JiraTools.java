package de.phib.jiratools;

import com.atlassian.jira.rest.client.api.domain.Issue;
import de.phib.jiratools.tools.CalculateRemainingEstimates;
import de.phib.jiratools.tools.GenerateReleaseNotes;

/**
 * Entrypoint for various small tools for searching for issues in JIRA and working with the search results.
 */
public class JiraTools {

    private JiraApiConnector jiraApiConnector;

    public JiraTools(String uri, String username, String password) {
        this.jiraApiConnector = new JiraApiConnector(uri, username, password);
    }

    public int calculateRemainingEstimates(String jql) {
        Iterable<Issue> issues = this.jiraApiConnector.searchIssues(jql);

        return CalculateRemainingEstimates.getRemainingEstimates(issues);
    }

    public String generateReleaseNotes(String jql) {
        Iterable<Issue> issues = this.jiraApiConnector.searchIssues(jql);

        return GenerateReleaseNotes.getReleaseNotes(issues);
    }

}
