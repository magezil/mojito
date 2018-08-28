package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.GitInfo;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.XcodeXliffFileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.TextUnitWithUsageClient;
import com.box.l10n.mojito.rest.entity.*;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"git-blame", "gb"}, commandDescription = "Git blame")
public class GitBlameCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    // TODO: add parameters to specify prefix from Mojito and prefix on localhost
    // localhost is in commandDirectories already? Mojito is /mnt/jenkins/workspace/webapp-l10n-string-extract atm
    String localhostPrefix = "/Users/emagalindan/code/pinboard/";
    String mojitoPrefix = "/mnt/jenkins/workspace/webapp-l10n-string-extract/";

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    TextUnitWithUsageClient textUnitWithUsageClient;

    @Autowired
    CommandHelper commandHelper;

    CommandDirectories commandDirectories;

    org.eclipse.jgit.lib.Repository gitRepository;

    @Override
    public void execute() throws CommandException {

        commandDirectories = new CommandDirectories(sourceDirectoryParam);

        consoleWriter.newLine().a("Git blame for repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(2);

        if (fileType.getSourceFileExtension().equals("pot")) {
            blameWithTextUnitUsages();
        } else {
            blameSourceFiles();
        }


        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    // ANDROID_STRINGS file
    void blameSourceFiles() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();
        List<TextUnitWithUsage> textUnitsToBlame = textUnitWithUsageClient.getTextUnitToBlame(repository.getId());
        GitInfoForTextUnits gitInfoForTextUnits = new GitInfoForTextUnits();
        List <GitInfoForTextUnit> gitInfoForTextUnitList = new ArrayList<>();

        ArrayList<FileMatch> sourceFileMatches = commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex);

        for (FileMatch sourceFileMatch : sourceFileMatches) {

            logger.info("file: {}", sourceFileMatch.getPath().toString());

            Path sourceRelativePath = getGitRepository().getDirectory().getParentFile().toPath().relativize(sourceFileMatch.getPath());

            BlameResult blameResultForFile = getBlameResultForFile(sourceRelativePath.toString());
//            logger.info("blame toString {}", blameResultForFile.getResultContents().toString());

            for (int i = 0; i < blameResultForFile.getResultContents().size(); i++) {
                String lineText = blameResultForFile.getResultContents().getString(i);

//                logger.info("Line text: {}", lineText);
//                logger.info("getTextUnitNames {}", getTextUnitNames(repository.getId()));
                String textUnitName = getTextUnitNameFromLine(lineText, textUnitsToBlame);
                if (textUnitName != null) {
                    getBlameResults(i, blameResultForFile);
                }
            }
        }
        gitInfoForTextUnits.setGitInfoForTextUnitList(gitInfoForTextUnitList); // pass this into post to save git information
    }

    // for po file
    void blameWithTextUnitUsages() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();
        List<TextUnitWithUsage> textUnitsToBlame = textUnitWithUsageClient.getTextUnitToBlame(repository.getId());

        logger.info("commandDirectories.sourceDirectoryPath {}", commandDirectories.getSourceDirectoryPath());
        logger.info("commandDirectories.targetDirectoryPath {}", commandDirectories.getTargetDirectoryPath());

        for (TextUnitWithUsage textUnitWithUsage : textUnitsToBlame) {
            for (String usage : textUnitWithUsage.getUsages()) {
                logger.info("textUnitName: {}\n usage: {}", textUnitWithUsage.getTextUnitName(), usage);
                String[] split = usage.split(":");
                String filename = split[0];
                int line = Integer.parseInt(split[1]) - 1; // need to account for line starting with 1 in file; 0 in array
                filename = filename.replace(mojitoPrefix, "");
                BlameResult blameResultForFile = getBlameResultForFile(filename);
                getBlameResults(line, blameResultForFile);

            }
        }

    }

    private void getBlameResults(int line, BlameResult blameResultForFile) {
        logger.info("blame result: {}", blameResultForFile.getResultContents().getString(line));
        logger.info("Author: {}, email: {}", blameResultForFile.getSourceAuthor(line).getName(), blameResultForFile.getSourceAuthor(line).getEmailAddress()); // name, email
        logger.info("commit: {}, time: {}", blameResultForFile.getSourceCommit(line).toString(), blameResultForFile.getSourceCommit(line).getCommitTime()); // commit id, date/time stamp
    }

    org.eclipse.jgit.lib.Repository getGitRepository() throws CommandException {

        if (gitRepository == null) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();

            try {
                gitRepository = builder
                        .findGitDir(new File(sourceDirectoryParam))
                        .readEnvironment()
                        .build();
            } catch (IOException ioe) {
                throw new CommandException("Can't build the git repository");
            }
        }

        return gitRepository;
    }

    BlameResult getBlameResultForFile(String filePath) throws CommandException {

        try {
            org.eclipse.jgit.lib.Repository gitRepository = getGitRepository();

            BlameCommand blamer = new BlameCommand(gitRepository);
            ObjectId commitID = gitRepository.resolve("HEAD");
            blamer.setStartCommit(commitID);
            blamer.setFilePath(filePath);
            BlameResult blame = blamer.call();

            return blame;
        } catch (GitAPIException | IOException e) {
            String msg = MessageFormat.format("Can't get blame result for file: {0}", filePath);
            logger.error(msg, e);
            throw new CommandException(msg, e);
        }
    }


    // textunit to find author
    String getTextUnitNameFromLine(String line, List<TextUnitWithUsage> textUnitWithUsages) {

        for (TextUnitWithUsage textUnitWithUsage : textUnitWithUsages) {
            if (line.contains(textUnitWithUsage.getTextUnitName())) {
                return textUnitWithUsage.getTextUnitName();
            }
        }
        return null;


//        for (int i = 0; i < line.length(); i++) {
//            for (int j = i + 1; j < line.length(); j++) {
//                for (TextUnitWithUsage textUnitWithUsage : textUnitWithUsages) {
//                    if (line.substring(i, j).equals(textUnitWithUsage.getTextUnitName())) {
//                        return textUnitWithUsage.getTextUnitName();
//                    }
//                }
//            }
//        }
//        return null;

    }

    /**
     * this is a dumb implementation. Need to check that the text unit is actual plural form. can't do that only
     * passing the name.
     *
     * @param textUnitName
     * @return
     */
    static String textUnitNameToStringInSourceFile(String textUnitName, boolean isPlural) {
        String stringInFile = textUnitName;

        if (isPlural) {
            stringInFile = textUnitName.split(" _(zero|one|two|few|many|other)")[0];
        }

        return stringInFile;
    }


}