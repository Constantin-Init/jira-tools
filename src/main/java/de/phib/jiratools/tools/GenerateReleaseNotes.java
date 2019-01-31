package de.phib.jiratools.tools;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides the functionality to generate release notes fora given list of issues.
 */
public class GenerateReleaseNotes {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateReleaseNotes.class);

    private GenerateReleaseNotes() {
        // static utility class without constructor
    }

    public static String getReleaseNotes(Collection<Issue> issues) {
        final StringBuilder releaseNotes = new StringBuilder();

        ReleaseNoteFormatter releaseNoteFormatter = new ReleaseNoteFormatter()
                .withStatus()
                .withAssignee()
                .withIssueType();

        Map<IssueType, List<Issue>> issuesByType = issues.stream()
                .collect(Collectors.groupingBy(Issue::getIssueType));
        issuesByType.forEach((issueType, issuesList) -> {
            releaseNotes.append(String.format("%n*%1$s*%n%n", issueType.getName()));
            for (Issue issue : issuesList) {
                releaseNotes.append(releaseNoteFormatter.format(issue));
            }
        });

        String result = releaseNotes.toString();
        LOG.info("Release Notes:\n\n{}", result);

        return result;
    }

    private static class ReleaseNoteFormatter {
        static final String FIELD_DELIMITER = "; ";
        private boolean showAssignee = false;
        private boolean showStatus = false;
        private boolean showIssueType = false;

        ReleaseNoteFormatter withAssignee() {
            this.showAssignee = true;
            return this;
        }

        ReleaseNoteFormatter withStatus() {
            this.showStatus = true;
            return this;
        }

        ReleaseNoteFormatter withIssueType() {
            this.showIssueType = true;
            return this;
        }

        String format(Issue issue) {
            StringBuilder sb = new StringBuilder();
            sb.append(issue.getKey())
                    .append(": ")
                    .append(issue.getSummary());
            if (showIssueType) {
                sb.append(FIELD_DELIMITER).append(issue.getIssueType().getName());
            }
            if (showAssignee) {
                User assignee = issue.getAssignee();
                sb.append(FIELD_DELIMITER).append(assignee != null ? assignee.getDisplayName() : "Unassigned");
            }
            if (showStatus) {
                sb.append(FIELD_DELIMITER).append(issue.getStatus().getName());
            }
            sb.append("\n");
            return sb.toString();
        }
    }

}
