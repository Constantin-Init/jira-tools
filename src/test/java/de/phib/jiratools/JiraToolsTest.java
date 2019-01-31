package de.phib.jiratools;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.phib.jgit.GitTools;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unit tests for the class JiraTools.
 */
public class JiraToolsTest {
    private static final Logger LOG = LoggerFactory.getLogger(JiraToolsTest.class);

    private static final String JIRA_PASSWORD = "password";
    private static final String JIRA_USERNAME = "username";

    private static final String GIT_ANSIBLE_REPO_PATH = "F:/bpa/ansible-workspace";
    private static final String GIT_DXP_REPO_PATH = "F:/bpa/dxp-blueprint";

    private static final String DXP_CURRENT_VERSION_TAG = "blueprint-2019.1.2";
    private static final String DXP_PREVIOUS_VERSION_TAG = "blueprint-2018.27.12";
    private static final String ANSIBLE_CURRENT_VERSION_TAG = "2019.1.2";
    private static final String ANSIBLE_PREVIOUS_VERSION_TAG = "2018.27.6";

    private static final List<String> JIRA_PROJECTS = Arrays.asList("BPA", "BREGNEU");
    private static final List<String> JIRA_VERSIONS = Arrays.asList("2019.1", "2019.1.1", "2019.1.2");

    private static JiraTools jiraTools;
    private static GitTools dxpGitTools;
    private static GitTools ansibleGitTools;

    /**
     * Creates an instance of JiraTools to be used in the unit tests.
     */
    @BeforeAll
    public static void setupTests() {
        jiraTools = new JiraTools("https://issues.init.de", JIRA_USERNAME, JIRA_PASSWORD);
        dxpGitTools = new GitTools(GIT_DXP_REPO_PATH);
        ansibleGitTools = new GitTools(GIT_ANSIBLE_REPO_PATH);
    }

    /**
     * Tests the method CalculateRemainingEstimates#getRemainingEstimates(java.lang.String).
     */
    @Test
    public void testCalculateRemainingEstimates() {
        int estimates = jiraTools.calculateRemainingEstimates("project = DEMO");

        Assertions.assertTrue(estimates > -1);
    }

    @Test
    public Map<String, Set<RevCommit>> getEffectedIssuesFromGit() {
        Map<String, Set<RevCommit>> effectedIssues = dxpGitTools.getEffectedIssues(DXP_PREVIOUS_VERSION_TAG, DXP_CURRENT_VERSION_TAG);
        Map<String, Set<RevCommit>> effectedAnsibleIssues = ansibleGitTools.getEffectedIssues(ANSIBLE_PREVIOUS_VERSION_TAG, ANSIBLE_CURRENT_VERSION_TAG);

        effectedAnsibleIssues.forEach((key, value) -> effectedIssues.merge(key, value, Sets::union));
        LOG.info("Git changed issues");
        effectedIssues.forEach((key, value) -> LOG.info("{} mentioned in commits {}", key, GitTools.getAbbrCommitList(value)));
        return effectedIssues;
    }

    @Test
    public Map<String, Issue> findChangedIssueKeysFromJira() {
        return new TreeMap<>(jiraTools.getIssuesForVersion(JIRA_VERSIONS, JIRA_PROJECTS));
    }


    @Test
    void findPublicReleaseIssues() {
        Map<String, Set<RevCommit>> effectedIssuesFromGit = getEffectedIssuesFromGit();
        Map<String, Issue> changedIssuesFromJira = jiraTools.getIssuesForVersion(JIRA_VERSIONS, JIRA_PROJECTS);

        Map<String, Issue> publicIssues = changedIssuesFromJira.values()
                .stream()
                .filter(jiraTools::isPublicIssue)
                .collect(Collectors.toMap(BasicIssue::getKey, i -> i));

        Set<String> jiraKeys = publicIssues.keySet();
        LOG.info("**Public Issues**");
        jiraKeys.forEach(i -> LOG.info("{}", i));

        Set<String> gitIssues = effectedIssuesFromGit.keySet();

        Sets.SetView<String> intersection = Sets.intersection(gitIssues, jiraKeys);
        ArrayList<String> publicIssuesWithCodeChanges = Lists.newArrayList(intersection);
        publicIssuesWithCodeChanges.sort(Comparator.naturalOrder());

        LOG.info("**Public Issues**");
        for (String i : publicIssuesWithCodeChanges) {
            Issue issue = publicIssues.get(i);
            String status = issue.getStatus().getName();
            String summary = issue.getSummary();
            String assignee = issue.getAssignee() != null ? issue.getAssignee().getDisplayName() : "Nicht zugewiesen";
            String issueType = issue.getIssueType().getName();
            String level = jiraTools.getIssueLevel(issue);
            LOG.info("{};\"{}\";{};{};{};{}", i, summary, issueType, level, status, assignee);
        }

    }

    @Test
    void findDiff() {
        Map<String, Set<RevCommit>> effectedIssues = getEffectedIssuesFromGit();
        Map<String, Issue> issueMap = findChangedIssueKeysFromJira();
        Set<String> jiraIssueKeys = issueMap.keySet();

        Set<String> gitIssueKeys = effectedIssues.keySet();

        Map<String, String> aliases = jiraTools.findAliases(gitIssueKeys);

        List<String> commitsNoTicketList = Sets.difference(gitIssueKeys, jiraIssueKeys)
                .stream()
                .filter(issue -> !aliases.containsKey(issue) || !jiraIssueKeys.contains(aliases.get(issue)))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        commitsNoTicketList.forEach(s -> LOG.warn("{}: mentioned in commit(s) {} but not tagged as fixed in version(s) {} in JIRA. Set FixVersions: {}. Status: {}",
                s,
                GitTools.getAbbrCommitList(effectedIssues.get(s)),
                String.join(",", JIRA_VERSIONS),
                jiraTools.getFixVersionsAsString(issueMap, s),
                jiraTools.getIssue(issueMap, s) != null ? jiraTools.getIssue(issueMap, s).getStatus().getName() : "doesn't exist"));

        ArrayList<String> jiraNoCommitList = Lists.newArrayList(Sets.difference(jiraIssueKeys, gitIssueKeys));
        jiraNoCommitList.sort(Comparator.naturalOrder());
        jiraNoCommitList.stream()
                .filter(issue -> !aliases.containsValue(issue))
                .forEach(s -> LOG.warn("{} mentioned as fixed in JIRA but no commits found", s));



    }


    /**
     * Tests the method CalculateRemainingEstimates#getRemainingEstimates(java.lang.String).
     */
    @Test
    public void testGenerateReleaseNotes() {
        List<String> statusList = Arrays.asList("Ready To Deploy", "QA Stage", "QA Stage BPA", "QA Prod", "QA Prod BPA", "Fertig", "Done");
        Map<String, Issue> issuesForVersion = jiraTools.getIssuesForVersion(JIRA_VERSIONS, JIRA_PROJECTS, statusList);
        jiraTools.generateReleaseNotes(issuesForVersion.values());
    }

}
