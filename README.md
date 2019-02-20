# jira-tools

Provides the functionality to execute a search for issues in JIRA based on a given JQL search query and return:
- the sum of the remaining estimates of the resulting issues
- release notes for the resulting issues

## Requirements

- Java 8

## How to Run

### Preparation

In `de.phib.ToolDataConstants`, replace the value of the following variables:
- JIRA_URL
- JIRA_USERNAME
- JIRA_PASSWORD
- GIT_DXP_REPO_PATH
- GIT_ANSIBLE_REPO_PATH
- GIT_SSH_PASSWORD (optional: passphrase for git ssh cert)

These are used for Test execution.


```
@BeforeAll
public static void setupTests() {
    jiraTools = new JiraTools(JIRA_URL, JIRA_USERNAME, JIRA_PASSWORD);
    dxpGitTools = new GitTools(GIT_DXP_REPO_PATH);
    ansibleGitTools = new GitTools(GIT_ANSIBLE_REPO_PATH);
}
```

### Remaining Estimates

In `de.phib.jiratools.JiraToolsTest`, replace the value of the variables `JIRA_REMAINING_ESTIMATES_QUERY`

```
@Test
public void testCalculateRemainingEstimates() {
    int estimates = jiraTools.calculateRemainingEstimates(JIRA_REMAINING_ESTIMATES_QUERY);

    Assertions.assertTrue(estimates > -1);
}
```

Then run the unit test `de.phib.jiratools.JiraToolsTest.testCalculateRemainingEstimates`. The sum of the
remaining estimates will be written to stdout, together with some further information.

Example:
```
[main] INFO de.phib.jiratools.JiraApiConnector - Starting search...
[main] INFO de.phib.jiratools.JiraApiConnector - Search query: project = DEMO
[main] INFO de.phib.jiratools.JiraApiConnector - Search finished.
[main] INFO de.phib.jiratools.JiraApiConnector - Number of results: 322
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Remaining estimates (in seconds): 7452000
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Remaining estimates (in working days): 258.75
```

### Release Notes

In `de.phib.jiratools.JiraToolsTest`, replace the value of the following variables:
- JIRA_VERSIONS
- JIRA_PROJECTS
- JIRA_STATUS_LIST

```
@Test
public void testGenerateReleaseNotes() {
    Map<String, Issue> issuesForVersion = jiraTools.getIssuesForVersion(JIRA_VERSIONS, JIRA_PROJECTS, JIRA_STATUS_LIST);
    jiraTools.generateReleaseNotes(issuesForVersion.values());
}
```

Then run the unit test `de.phib.jiratools.JiraToolsTest.testGenerateReleaseNotes`. The sum of the
remaining estimates will be written to stdout, together with some further information.

Example:
```
[main] INFO de.phib.jiratools.JiraApiConnector - Starting search...
[main] INFO de.phib.jiratools.JiraApiConnector - Search query: project = DEMO AND fixVersion = 1.0 AND level = "public" ORDER BY issuetype DESC, key ASC
[main] INFO de.phib.jiratools.JiraApiConnector - Search finished.
[main] INFO de.phib.jiratools.JiraApiConnector - Number of results: 3
[main] INFO de.phib.jiratools.tools.GenerateReleaseNotes - Release Notes:

DEMO-1: Issue #1
DEMO-2: Issue #2
DEMO-3: Issue #3
```

## License

[MIT](LICENSE)

## Author Information

Created by [Philippe Boessling](https://www.gihub.com/pboessling).