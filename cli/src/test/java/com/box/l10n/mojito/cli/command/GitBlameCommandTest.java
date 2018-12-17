package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.entity.GitBlame;
import com.box.l10n.mojito.rest.entity.GitBlameWithUsage;
import com.box.l10n.mojito.service.gitblame.GitBlameService;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author emagalindan
 */
public class GitBlameCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommandTest.class);

    @Autowired
    GitBlameService gitBlameService;

    /**
     * Travis does shallow clone, which prevents "integration" tests looking up commits since they are too old
     * to test locally this can be set to false.
     */
    boolean shallowClone = true;

    @Test
    public void android() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

        for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            assertNull(gitBlameWithUsage.getGitBlame());
        }

        GitBlame gitBlame = new GitBlame();
        gitBlame.setCommitName("37801193683d2e852a8a2b81e6dd05ca9ed13598");
        gitBlame.setCommitTime("1537568049");
        gitBlame.setAuthorName("Jean Aurambault");
        gitBlame.setAuthorEmail("aurambaj@users.noreply.github.com");

        getL10nJCommanderWithSpiedIfShallow(gitBlame).run("git-blame", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
        verifyGitBlame(gitBlameWithUsages, gitBlame);
    }

    @Test
    public void poFile() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");

        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);

        for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            assertNull(gitBlameWithUsage.getGitBlame());
        }

        GitBlame gitBlame = new GitBlame();
        gitBlame.setCommitName("1a86b8a2003f4d20858bfb53770119f039520f79");
        gitBlame.setCommitTime("1537572147");
        gitBlame.setAuthorName("Liz Magalindan");
        gitBlame.setAuthorEmail("emagalindan@pinterest.com");

        L10nJCommander l10nJCommanderWithSpiedIfShallow = getL10nJCommanderWithSpiedIfShallow(gitBlame);
        l10nJCommanderWithSpiedIfShallow.run("git-blame", "-r", repository.getName(),
                "-s", sourceDirectory.getAbsolutePath(),
                "-ft", "po");

        assertEquals(0, l10nJCommanderWithSpiedIfShallow.getExitCode());

        gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
        verifyGitBlame(gitBlameWithUsages, gitBlame);
    }


    @Test
    public void getStringInSourceFile() {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_zero"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_one"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_two"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_few"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_many"));
        assertEquals("test", gitBlameCommand.textUnitNameToStringInSourceFile("test_other"));
        assertEquals("test_test", gitBlameCommand.textUnitNameToStringInSourceFile("test_test"));
        assertEquals("test_test", gitBlameCommand.textUnitNameToStringInSourceFile("test_test_one"));
    }

    @Test
    public void getTextUnitName() {
        String lines[] = new String[]{
                "<string name=\"test_0\">Test 0</string>\n",
                "    <string name=\"test_1\">Test 1</string>"};
        GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameWithUsage.setTextUnitName("test_0");
        GitBlameWithUsage gitBlameWithUsage1 = new GitBlameWithUsage();
        gitBlameWithUsage1.setTextUnitName("test_1");

        List<GitBlameWithUsage> textUnitWithUsages = new ArrayList<>();
        textUnitWithUsages.add(gitBlameWithUsage);
        textUnitWithUsages.add(gitBlameWithUsage1);

        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        for (int i = 0; i < lines.length; i++) {
            List<GitBlameWithUsage> gitBlameWithUsages = gitBlameCommand.getGitBlameWithUsagesFromLine(lines[i], textUnitWithUsages);
            assertEquals(textUnitWithUsages.get(i), gitBlameWithUsages.get(0));
            assertEquals(1, gitBlameWithUsages.size());
        }
    }

    @Test
    public void getTextUnitNamePlural() {
        String line = "<plurals name=\"plural_tests\">\n";
        GitBlameWithUsage gitBlameWithUsage_zero = new GitBlameWithUsage();
        gitBlameWithUsage_zero.setTextUnitName("plural_tests_zero");
        GitBlameWithUsage gitBlameWithUsage_one = new GitBlameWithUsage();
        gitBlameWithUsage_one.setTextUnitName("plural_tests_one");
        GitBlameWithUsage gitBlameWithUsage_two = new GitBlameWithUsage();
        gitBlameWithUsage_two.setTextUnitName("plural_tests_two");
        GitBlameWithUsage gitBlameWithUsage_few = new GitBlameWithUsage();
        gitBlameWithUsage_few.setTextUnitName("plural_tests_few");
        GitBlameWithUsage gitBlameWithUsage_many = new GitBlameWithUsage();
        gitBlameWithUsage_many.setTextUnitName("plural_tests_many");
        GitBlameWithUsage gitBlameWithUsage_other = new GitBlameWithUsage();
        gitBlameWithUsage_other.setTextUnitName("plural_tests_other");

        List<GitBlameWithUsage> gitBlameWithUsagesExpected = new ArrayList<>();
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_zero);
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_one);
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_two);
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_few);
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_many);
        gitBlameWithUsagesExpected.add(gitBlameWithUsage_other);

        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        List<GitBlameWithUsage> gitBlameWithUsagesActual = gitBlameCommand.getGitBlameWithUsagesFromLine(line, gitBlameWithUsagesExpected);

        for (int i = 0; i < gitBlameWithUsagesActual.size(); i++)
            assertEquals(gitBlameWithUsagesExpected.get(i), gitBlameWithUsagesActual.get(i));
    }


    @Test
    public void getTextUnitNameStringArray() {
        String string_name_0 = "string_array_tests_0";
        String string_name_1 = "string_array_tests_1";
        String string_name_2 = "string_array_tests_2";
        String string_name_3 = "string_array_tests_3";
        String string_name_4 = "string_array_tests_4";
        String string_name_5 = "string_array_tests_5";

        List<String> stringArrayNames = new ArrayList<>();
        stringArrayNames.add(string_name_0);
        stringArrayNames.add(string_name_1);
        stringArrayNames.add(string_name_2);
        stringArrayNames.add(string_name_3);
        stringArrayNames.add(string_name_4);
        stringArrayNames.add(string_name_5);

        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        for (int i = 0; i < stringArrayNames.size(); i++)
            assertEquals("string_array_tests", gitBlameCommand.textUnitNameToStringInSourceFile(stringArrayNames.get(i)));
    }

    @Test
    public void getBlameResultForLines() throws Exception {

        if (shallowClone) {
            // that won't work on a shallow clone / on travis. Keep it for local testing
            return;
        }

        File sourceDirectory = getInputResourcesTestDir("source");
        String filepath = sourceDirectory.getAbsolutePath();

        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(filepath);
        gitBlameCommand.initGitRepository();

        String relativePath = gitBlameCommand.gitRepository.getDirectory().toPath().getParent().relativize(sourceDirectory.toPath()).toString();
        relativePath = relativePath + "/res/values/strings.xml";
        BlameResult blameResult = gitBlameCommand.gitRepository.getBlameResultForFile(relativePath);

        // Will not hold up if file is committed by another person and/or at another time
        String expectedAuthor = "Liz Magalindan";
        String expectedEmail = "256@holbertonschool.com";
        String expectedSourceCommit = "88025e7b8b0f5d0f12f90c4ed9f86623074bc2ee";
        int expectedTime = 1537477876;
        for (int lineNumber = 0; lineNumber < blameResult.getResultContents().size(); lineNumber++) {
            PersonIdent actualAuthor = blameResult.getSourceAuthor(lineNumber);
            RevCommit actualCommit = blameResult.getSourceCommit(lineNumber);
            assertEquals(expectedAuthor, actualAuthor.getName());
            assertEquals(expectedEmail, actualAuthor.getEmailAddress());
            assertEquals(expectedSourceCommit, actualCommit.getName());
            assertEquals(expectedTime, actualCommit.getCommitTime());
        }
    }

    @Test
    public void getFileName() {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        assertEquals("file.js", gitBlameCommand.getFileName("file.js"));
        assertEquals("file.js", gitBlameCommand.getFileName("file.js:25"));
        assertEquals("path/to/file.js", gitBlameCommand.getFileName("path/to/file.js"));
        assertEquals("path/to/file.js", gitBlameCommand.getFileName("path/to/file.js:25"));
    }

    @Test
    public void getFileLine() throws Exception {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        assertEquals(24, gitBlameCommand.getLineNumber("file.js:25"));
        assertEquals(24, gitBlameCommand.getLineNumber("path/to/file.js:25"));
    }

    @Test
    public void getBlameResultForFileWhenFileIsMissing() throws CommandException, NoSuchFileException {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(getInputResourcesTestDir().getAbsolutePath());
        gitBlameCommand.initGitRepository();
        BlameResult blameResult = gitBlameCommand.gitRepository.getBlameResultForFile("forSomeMissingFile");
        assertNull(blameResult);
    }


    @Test(expected = NoSuchFileException.class)
    public void getBlameResultForFileCachedWhenFileIsMissing() throws CommandException, NoSuchFileException {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(getInputResourcesTestDir().getAbsolutePath());
        gitBlameCommand.initGitRepository();
        gitBlameCommand.getBlameResultForFileCached("forSomeMissingFile");
    }

    @Test(expected = LineMissingException.class)
    public void updateGitBlameOutOfBousnd() throws CommandException, NoSuchFileException, LineMissingException {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(getBaseDir().getAbsolutePath());
        gitBlameCommand.initGitRepository();
        BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("pom.xml");
        GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameCommand.updateBlameResultsInGitBlameWithUsage(100000, blameResult, gitBlameWithUsage);
        assertNull(gitBlameWithUsage.getGitBlame().getAuthorName());
    }

    @Test(expected = NoSuchFileException.class)
    public void updateGitBlameOMissingFile() throws CommandException, NoSuchFileException, LineMissingException {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(getBaseDir().getAbsolutePath());
        gitBlameCommand.initGitRepository();
        BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("somemissginfile");
        GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
        gitBlameCommand.updateBlameResultsInGitBlameWithUsage(10, blameResult, gitBlameWithUsage);
        assertNull(gitBlameWithUsage.getGitBlame().getAuthorName());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void getSourceCommitsAccessOutOfBound() throws CommandException, NoSuchFileException {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();
        gitBlameCommand.commandDirectories = new CommandDirectories(getBaseDir().getAbsolutePath());
        gitBlameCommand.initGitRepository();
        BlameResult blameResult = gitBlameCommand.getBlameResultForFileCached("pom.xml");
        blameResult.getSourceCommit(100000);
    }

    @Test
    public void getGitBlameWithUsagesToProcess() {
        GitBlameCommand gitBlameCommand = new GitBlameCommand();

        ArrayList<GitBlameWithUsage> gitBlameWithUsages = new ArrayList<>();
        GitBlameWithUsage toSkip = new GitBlameWithUsage();
        toSkip.setGitBlame(new GitBlame());
        gitBlameWithUsages.add(toSkip);
        gitBlameWithUsages.add(new GitBlameWithUsage());

        List<GitBlameWithUsage> gitBlameWithUsagesToProcess = gitBlameCommand.getGitBlameWithUsagesToProcess(gitBlameWithUsages);

        assertEquals(1, gitBlameWithUsagesToProcess.size());
        assertFalse(gitBlameWithUsagesToProcess.contains(toSkip));
    }

    void verifyGitBlame(List<com.box.l10n.mojito.service.gitblame.GitBlameWithUsage> gitBlameWithUsages, GitBlame gitBlame) {
        assertFalse(gitBlameWithUsages.isEmpty());
        for (com.box.l10n.mojito.service.gitblame.GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            assertEquals(gitBlame.getCommitName(), gitBlameWithUsage.getGitBlame().getCommitName());
            assertEquals(gitBlame.getCommitTime(), gitBlameWithUsage.getGitBlame().getCommitTime());
            assertEquals(gitBlame.getAuthorName(), gitBlameWithUsage.getGitBlame().getAuthorName());
            assertEquals(gitBlame.getAuthorEmail(), gitBlameWithUsage.getGitBlame().getAuthorEmail());
        }
    }

    L10nJCommander getL10nJCommanderWithSpiedIfShallow(GitBlame gitBlame) throws Exception {
        L10nJCommander l10nJCommander = getL10nJCommander();

        if (shallowClone) {
            GitBlameCommand gitBlameCommand = l10nJCommander.getCommand(GitBlameCommand.class);
            gitBlameCommand.gitRepository = spy(GitRepository.class);
            doReturn(gitBlame).when(gitBlameCommand.gitRepository).getBlameResults(anyInt(), any());
        }

        return l10nJCommander;
    }
}