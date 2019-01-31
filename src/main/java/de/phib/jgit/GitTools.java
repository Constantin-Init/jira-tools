package de.phib.jgit;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GitTools {
    private static final String ISSUE_REGEX = "([A-Z]+-[0-9]+)";
    private static final Logger LOG = LoggerFactory.getLogger(GitTools.class);
    private final String path;

    public GitTools(String path) {
        this.path = path;
    }

    /**
     * Returns an abbreviated list of commit hashes
     * @param value
     * @return
     */
    @NonNull
    public static String getAbbrCommitList(@NonNull Set<RevCommit> value) {
        return value.stream()
                .map(c -> c.getId().abbreviate(10).name())
                .collect(Collectors.joining(", "));
    }

    /***
     * Computes all effected JIRA issues between two Git Tags by trying to extract the issuekey from the commit message
     * @param lastReleaseTag name of the last release tag
     * @param currentReleaseTag name of the current release tag
     * @return Map where the key is the issueKey and the value is a set of commits that include the issueKey.
     */
    public Map<String, Set<RevCommit>> getEffectedIssues(String lastReleaseTag, String currentReleaseTag) {

        File gitDir = new File(path);

        try (Git gitRepo = Git.open(gitDir)) {
            gitRepo.fetch().call();
            List<Ref> tags = gitRepo.tagList().call();
            Optional<Ref> lastReleaseTagRef = tags.stream().filter(ref -> ref.getName().contains(lastReleaseTag)).findFirst();
            Optional<Ref> currentReleaseTagRef = tags.stream().filter(ref -> ref.getName().contains(currentReleaseTag)).findFirst();

            if (!lastReleaseTagRef.isPresent()) {
                LOG.error("Couldn't find lastReleaseTag {}", lastReleaseTag);
                return Maps.newHashMap();
            }

            if (!currentReleaseTagRef.isPresent()) {
                LOG.error("Couldn't find currentRelease Tag {}", currentReleaseTag);
                return Maps.newHashMap();
            }

            Pattern pattern = Pattern.compile(ISSUE_REGEX);

            return StreamSupport.stream(gitRepo.log()
                    .addRange(getActualRefObjectId(lastReleaseTagRef.get(), gitRepo.getRepository()), getActualRefObjectId(currentReleaseTagRef.get(), gitRepo.getRepository()))
                    .call()
                    .spliterator(), false)
                    .map(ref -> {
                        Matcher matcher = pattern.matcher(ref.getFullMessage());
                        List<Pair<String, RevCommit>> commits = new ArrayList<>();
                        while (matcher.find()) {
                            commits.add(new Pair<>(matcher.group(), ref));
                        }
                        commits.sort(Comparator.comparingInt(p -> p.getSecond().getCommitTime()));
                        return commits;
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(Pair::getFirst,
                            TreeMap::new,
                            Collectors.mapping(Pair::getSecond, Collectors.toSet())));

        } catch (IOException e) {
            LOG.error("Couldn't open Git Repo at {}", this.path, e);
            return Maps.newHashMap();
        } catch (GitAPIException e) {
            LOG.error("Exception calling Git API", e);
        }
        return Maps.newHashMap();
    }

    private ObjectId getActualRefObjectId(Ref ref, Repository repo) throws IOException {
        final Ref repoPeeled = repo.getRefDatabase().peel(ref);
        if (repoPeeled.getPeeledObjectId() != null) {
            return repoPeeled.getPeeledObjectId();
        }
        return ref.getObjectId();
    }

    private class Pair<T, U> {
        T first;
        U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() {
            return first;
        }

        public U getSecond() {
            return second;
        }
    }

}
