package org.safehaus.subutai.core.git.impl;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.core.command.api.CommandRunner;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.core.git.api.GitChangedFile;
import org.safehaus.subutai.core.git.api.GitException;
import org.safehaus.subutai.core.git.api.GitFileStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test for GitManagerImpl
 */
public class GitManagerImplTest
{
    private static final String MASTER_BRANCH = "master";
    private static final String DUMMY_BRANCH = "dummy";
    private static final String REPOSITORY_ROOT = "root";
    private static final String MODIFIED_FILE_PATH =
            "management/server/core/communication-manager/communication-manager-api/src/main/java/org/safehaus"
                    + "/subutai/core/communication/api/CommandJson.java";

    private static final String DIFF_BRANCH_OUTPUT = String.format( "M %s", MODIFIED_FILE_PATH );
    private static final String DIFF_FILE_OUTPUT = "+new content\n-old content";
    private static final String FILE_PATH = "some/file/path";
    private static final String SYS_OUT = "some dummy output";


    @Test( expected = NullPointerException.class )
    public void constructorShouldFailOnNullCommandRunner()
    {
        new GitManagerImpl( null );
    }


    @Test
    public void shouldPrintToSysOut() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, true, agent.getUuid(), SYS_OUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        //catch sys out
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        System.setOut( new PrintStream( myOut ) );

        gitManager.init( agent, REPOSITORY_ROOT );

        assertEquals( SYS_OUT, myOut.toString().trim() );
    }


    @Test(expected = GitException.class)
    public void shouldThrowGitException() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, false, agent.getUuid(), SYS_OUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        gitManager.init( agent, REPOSITORY_ROOT );

    }


    @Test
    public void shouldReturnDiffBranchWithMasterBranch() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, true, agent.getUuid(), DIFF_BRANCH_OUTPUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        List<GitChangedFile> changedFiles = gitManager.diffBranches( agent, REPOSITORY_ROOT, MASTER_BRANCH );
        GitChangedFile changedFile = changedFiles.get( 0 );

        assertTrue( changedFiles.contains( new GitChangedFile( GitFileStatus.MODIFIED, MODIFIED_FILE_PATH ) ) );
        assertEquals( GitFileStatus.MODIFIED, changedFile.getGitFileStatus() );
        assertEquals( MODIFIED_FILE_PATH, changedFile.getGitFilePath() );
    }


    @Test
    public void shouldReturnDiffBranches() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, true, agent.getUuid(), DIFF_BRANCH_OUTPUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        List<GitChangedFile> changedFiles =
                gitManager.diffBranches( agent, REPOSITORY_ROOT, MASTER_BRANCH, DUMMY_BRANCH );
        GitChangedFile changedFile = changedFiles.get( 0 );

        assertTrue( changedFiles.contains( new GitChangedFile( GitFileStatus.MODIFIED, MODIFIED_FILE_PATH ) ) );
        assertEquals( GitFileStatus.MODIFIED, changedFile.getGitFileStatus() );
        assertEquals( MODIFIED_FILE_PATH, changedFile.getGitFilePath() );
    }


    @Test
    public void shouldReturnDiffFile() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, true, agent.getUuid(), DIFF_FILE_OUTPUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        String diffFile = gitManager.diffFile( agent, REPOSITORY_ROOT, MASTER_BRANCH, DUMMY_BRANCH, FILE_PATH );

        assertEquals( diffFile, DIFF_FILE_OUTPUT );
    }


    @Test
    public void shouldReturnDiffFileWithMasterBranch() throws GitException
    {
        Agent agent = MockUtils.getAgent( UUID.randomUUID() );
        Command command = MockUtils.getCommand( true, true, agent.getUuid(), DIFF_FILE_OUTPUT, null, null );
        CommandRunner commandRunner = MockUtils.getCommandRunner( command );

        GitManagerImpl gitManager = new GitManagerImpl( commandRunner );

        String diffFile = gitManager.diffFile( agent, REPOSITORY_ROOT, MASTER_BRANCH, FILE_PATH );

        assertEquals( diffFile, DIFF_FILE_OUTPUT );
    }
}
