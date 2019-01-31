package de.phib.jiratools;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.Version;
import de.phib.jiratools.tools.CalculateRemainingEstimates;
import de.phib.jiratools.tools.GenerateReleaseNotes;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Entrypoint for various small tools for searching for issues in JIRA and working with the search results.
 */
public class JiraTools {

    private static final String SECURITY_LEVEL_PUBLIC = "public";
    private JiraApiConnector jiraApiConnector;

    public JiraTools(String uri, String username, String password) {
        this.jiraApiConnector = new JiraApiConnector(uri, username, password);
    }

    public int calculateRemainingEstimates(String jql) {
        Iterable<Issue> issues = this.jiraApiConnector.searchIssues(jql);

        return CalculateRemainingEstimates.getRemainingEstimates(issues);
    }

    public String generateReleaseNotes(Collection<Issue> issues) {
        return GenerateReleaseNotes.getReleaseNotes(issues);
    }

    public JiraApiConnector getJiraApiConnector() {
        return jiraApiConnector;
    }

    /**
     * Fetches an Issue from JIRA by key and returns the key.
     * This is useful for finding Issues that were moved from one project to another, because the key will have changed.
     *
     * @param key The requested key
     * @return the current key, or {@code key} if the issue can't be found}
     */
    @Nullable
    public String getCurrentKey(@NonNull String key) {
        Issue issue = getIssueByKey(key);
        if (issue != null) {
            return issue.getKey();
        }
        // TODO Maybe returning null if the key can't be found is better?
        return key;
    }

    /**
     * Fetches an Issue from JIRA by it's key
     *
     * @param key the issue key
     * @return the JIRA issue or null if the issue doesn't exist
     */
    @Nullable
    public Issue getIssueByKey(@NonNull String key) {
        Iterable<Issue> issues = getJiraApiConnector().searchIssues(String.format("key = %s", key));
        if (issues.iterator().hasNext()) {
            return issues.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Extracts the visibility / security level of the issue
     *
     * @param issue
     * @return
     */
    @NonNull
    String getIssueLevel(@NonNull Issue issue) {
        IssueField security = issue.getField("security");
        String name = "";
        if (security != null) {

            Object value = security.getValue();
            if (value instanceof JSONObject) {
                try {
                    JSONObject jsonObject = (JSONObject) value;
                    name = (String) jsonObject.get("name");
                } catch (JSONException e) {
                    // Do Nothing
                }
            }

        }
        return name;
    }

    /**
     * Fetches all Issues from JIRA
     *
     * @param versions  List of JIRA version to filter for
     * @param projects List of Projects to filter for
     * @param status
     * @param status
     * @return A map of issueKeys to issues
     */
    @NonNull
    public Map<String, Issue> getIssuesForVersion(@NonNull List<String> versions, @NonNull Iterable<String> projects, List<String> status) {
        Map<String, Issue> result = new HashMap<>();
        String fixedInTags = versions.stream()
                        .map(s -> String.format("fixed-in-%s", s))
                        .collect(Collectors.joining(","));
        for (String project : projects) {
            String jql = String.format("project IN (%1$s) ", project);
            if (!versions.isEmpty()) {
                jql += String.format("AND (labels IN (%1$s) OR fixVersion IN (%2$s)) ", fixedInTags, String.join(", ", versions));
                jql += "AND (labels NOT IN (\"ignore-in-release-checklist\") OR labels is EMPTY) ";
            }
            if (!status.isEmpty()) {
                String statusjoined = status.stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(","));
                jql += "AND status IN ( " + statusjoined + ") AND level = \"public\" ";

            }

            jql += "ORDER BY key ASC";
            Iterable<Issue> issues = getJiraApiConnector().searchIssues(jql);
            Map<String, Issue> issueMap = StreamSupport.stream(issues.spliterator(), false).collect(Collectors.toMap(BasicIssue::getKey, i -> i));
            result.putAll(issueMap);
        }

        return result;
    }

    public Map<String, Issue> getIssuesForVersion(@NonNull List<String> versions, @NonNull Iterable<String> projects) {
        return getIssuesForVersion(versions, projects, Collections.emptyList());
    }


        /**
         * Gets an issue from JIRA. Checks the {@code alreadyKnownIssues} first, before making an API request
         *
         * @param alreadyKnownIssues List of already known JIRA issues. The issue will be added if absent.
         * @param key                The issue to find
         * @return the issue.
         */
    Issue getIssue(@NonNull Map<String, Issue> alreadyKnownIssues, @NonNull String key) {
        return alreadyKnownIssues.computeIfAbsent(key, this::getIssueByKey);
    }

    /**
     * Creates a Map of issues and their "real" issue key. I.e. if an issue was moved to another Project the issue key
     * OLDPROJECT-123 might have a "real" issue key of NEWPROJECT-456
     * <p>
     * Note: Due to JIRA API limitations we need to make one request for each issue key. Could be expensive!
     *
     * @param gitIssueKeys A Set of issuekeys to look up
     * @return Map of old-keys to real-keys
     */
    @NonNull
    Map<String, String> findAliases(@NonNull Set<String> gitIssueKeys) {
        return gitIssueKeys.stream()
                .distinct()
                .collect(Collectors.toMap(i -> i, this::getCurrentKey));
    }

    /**
     * Checks the security level of an issue
     *
     * @param issue the issue
     * @return {@code true} if the issue has the security level "public"
     */
    boolean isPublicIssue(@NonNull Issue issue) {
        String name = getIssueLevel(issue);
        return SECURITY_LEVEL_PUBLIC.equals(name);

    }

    /**
     * Computes the fixVersion(s) for an issue.
     * While attempt to fetch the issue from the JIRA API if necessary.
     *
     * @param alreadyKnownIssues List of already known JIRA issues. The issue will be added if absent.
     * @param key                The issue to find
     * @return the fixVersion(s) as a string. Joined by {@code ', '} or null if the issue was not found.
     */
    @NonNull
    String getFixVersionsAsString(@Nonnull Map<String, Issue> alreadyKnownIssues, @NonNull String key) {
        Issue issue = getIssue(alreadyKnownIssues, key);
        if (issue != null) {
            Iterable<Version> fixVersions = issue.getFixVersions();
            if (fixVersions != null) {
                return StreamSupport.stream(fixVersions.spliterator(), false)
                        .map(Version::getName)
                        .collect(Collectors.joining(", "));
            }
        }

        return "";
    }
}
