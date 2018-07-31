package de.phib.jiratools.tools;

import com.atlassian.jira.rest.client.api.domain.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality to generate release notes fora given list of issues.
 */
public class GenerateReleaseNotes {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateReleaseNotes.class);

    private static String getKeyAndSummary(Issue issue) {
        String key = issue.getKey();
        String summary = issue.getSummary();

        return key + ": " + summary;
    }

    public static String getReleaseNotes(Iterable<Issue> issues) {
        String releaseNotes = "";
        for (Issue issue : issues) {
            releaseNotes += getKeyAndSummary(issue) + "\n";
        }

        LOG.info("Release Notes:\n\n" + releaseNotes);

        return releaseNotes;
    }

}
