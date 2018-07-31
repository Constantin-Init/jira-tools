# jira-tools

Provides the functionality to execute a search for issues in JIRA based on a given JQL search query and return:
- the sum of the remaining estimates of the resulting issues
- release notes for the resulting issues

## Requirements

- Java 8

## How to Run

### Preparation

Replace the values of parameters `uri`, `username`, and `password` in method 
`de.phib.jiratools.JiraToolsTest.setupTests` with the connection details for your JIRA instance.

```
@BeforeAll
public static void setupTests() {
    jiraTools = new JiraTools("http://example.com", "jirauser", "secret");
}
```

### Remaining Estimates

Replace the value of parameter `jql` in method `testCalculateRemainingEstimates` with your search query.

```
@Test
public void testCalculateRemainingEstimates() {
    int estimates = jiraTools.calculateRemainingEstimates("project = DEMO");

    Assertions.assertTrue(estimates > -1);
}
```

Then run the unit test `de.phib.jiratools.JiraToolsTest.testCalculateRemainingEstimates`. The sum of the
remaining estimates will be written to stdout, together with some further information.

Example:
```
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Starting search...
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Search query: project = DEMO
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Search finished.
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Number of results: 322
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Remaining estimates (in seconds): 7452000
[main] INFO de.phib.jiratools.CalculateRemainingEstimates - Remaining estimates (in working days): 258.75
```

### Release Notes

Replace the value of parameter `jql` in method `testGenerateReleaseNotes` with your search query.

```
@Test
public void testGenerateReleaseNotes() {
    String releaseNotes = jiraTools.generateReleaseNotes("project = DEMO AND fixVersion = 1.0 AND level = \"public\" ORDER BY issuetype DESC, key ASC");

    Assertions.assertTrue(releaseNotes != null);
}
```

Then run the unit test `de.phib.jiratools.JiraToolsTest.testGenerateReleaseNotes`. The sum of the
remaining estimates will be written to stdout, together with some further information.

Example:
```
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Starting search...
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Search query: project = DEMO AND fixVersion = 1.0 AND level = "public" ORDER BY issuetype DESC, key ASC
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Search finished.
[main] INFO de.phib.jiratools.tools.CalculateRemainingEstimates - Number of results: 3
[main] INFO de.phib.jiratools.tools.GenerateReleaseNotes - Release Notes:

DEMO-1: Issue #1
DEMO-2: Issue #2
DEMO-3: Issue #3
```

## License

[MIT](LICENSE)

## Author Information

Created by [Philippe Boessling](https://www.gihub.com/pboessling).