/*
package com.cx.plugin.cli.configascode;

import com.google.common.io.Files;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.stream.Stream;

public class GitRemoteService {
    private Git GIT;
    private static Logger log = LoggerFactory.getLogger(GitRemoteService.class);


    public GitRemoteService() {
    }

    public String cloneAndGetWorkingDirPath(String remoteUrl, String remoteBranch, String userName, String pass) {
        File temp = Files.createTempDir();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setBranch(remoteBranch)
                .setURI(remoteUrl)
                .setDirectory(temp);

        if( StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(pass)){
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, pass));
        }

        try {
            GIT = cloneCommand.call();
            log.debug("clone complete into : ",temp.getAbsolutePath());
        } catch (GitAPIException e) {
            closeAndDelete();
            log.warn("couldn't load config file from remote for the following reason : %s",e.getCause());
            return "";
        }

        return temp.getAbsolutePath();

    }

    public String cloneAndGetWorkingDirPath(String remoteUrl, String remoteBranch, String privateKeyPath){
        File temp = Files.createTempDir();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setBranch(remoteBranch)
                .setTransportConfigCallback(new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        if(transport instanceof SshTransport){
                            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                                @Override
                                protected void configure(OpenSshConfig.Host host, Session session) {
                                    session.setConfig("StrictHostKeyChecking", "no");
                                }

                                @Override
                                protected JSch createDefaultJSch(FS fs) throws JSchException {
                                    JSch jSch = super.createDefaultJSch(fs);
                                    jSch.addIdentity(privateKeyPath, "super-secret-passphrase".getBytes());
                                    return jSch;
                                }
                            };

                            ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
                        }
                    }
                })
                .setURI(remoteUrl)
                .setDirectory(temp);



        try {
            GIT = cloneCommand.call();
            log.debug("clone complete into : ",temp.getAbsolutePath());
        } catch (GitAPIException e) {
            closeAndDelete();
            log.warn("couldn't load config file from remote for the following reason : %s",e.getCause());
            return "";
        }

        return temp.getAbsolutePath();
    }

    public void closeAndDelete() {
        if(GIT == null )
            return;

        final File directory = GIT.getRepository().getWorkTree();
        GIT.close();
        delete(directory);
    }

    private void delete(File directory) {
        final File[] files;
        if (directory.isDirectory() && (files = directory.listFiles()) != null)
            Stream.of(files).forEach(this::delete);
        if (!directory.delete())
            log.debug("Could not delete file: " + directory);
    }
}
*/
