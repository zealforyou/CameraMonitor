/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ftpserver.filesystem.tfp.impl;

import org.apache.ftpserver.filesystem.tfp.UsbFileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * <strong>Internal class, do not use directly.</strong>
 * <p>
 * File system view based on native file system. Here the root directory will be
 * user virtual root (/).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class UsbFileSystemView implements FileSystemView {

    private final Logger LOG = LoggerFactory
            .getLogger(UsbFileSystemView.class);


    // the root directory will always end with '/'.
    private UsbFile rootDir;

    // the first and the last character will always be '/'
    // It is always with respect to the root directory.
    private String currDir;

    private User user;

    // private boolean writePermission;

    private boolean caseInsensitive = false;

    /**
     * Constructor - internal do not use directly, use {@link UsbFileSystemFactory} instead
     */
    protected UsbFileSystemView(User user) throws FtpException {
        this(user, false);
    }

    /**
     * Constructor - internal do not use directly, use {@link UsbFileSystemFactory} instead
     */
    public UsbFileSystemView(User user, boolean caseInsensitive)
            throws FtpException {
        if (user == null) {
            throw new IllegalArgumentException("user can not be null");
        }
        if (user.getUsbFileHome() == null) {
            throw new IllegalArgumentException(
                    "User home directory can not be null");
        }

        this.caseInsensitive = caseInsensitive;

        // add last '/' if necessary
        UsbFile rootDir = user.getUsbFileHome();
        LOG.debug("Native filesystem view created for user \"{}\" with root \"{}\"", user.getName(), rootDir);

        this.rootDir = rootDir;

        this.user = user;

        currDir = "/";
    }

    /**
     * Get the user home directory. It would be the file system root for the
     * user.
     */
    @Override
    public FtpFile getHomeDirectory() throws FtpException {
        return new UsbFtpFile("/", rootDir, user);
    }

    /**
     * Get the current directory.
     */
    public FtpFile getWorkingDirectory() {
        FtpFile fileObj = null;
        if (currDir.equals("/")) {
            fileObj = new UsbFtpFile("/", rootDir, user);
        } else {
            UsbFile file = null;
            try {
                file = rootDir.search(currDir.substring(1));
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileObj = new UsbFtpFile(currDir, file, user);

        }
        return fileObj;
    }

    /**
     * Get file object.
     */
    public FtpFile getFile(String file) {

        // get actual file object
        UsbFile physicalFile = UsbFtpFile.getPhysicalFile(rootDir,
                currDir, file, caseInsensitive);
        if (physicalFile == null) {
            return null;
        }
        // strip the root directory and return
        String userFileName = physicalFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() - 1);
        return new UsbFtpFile(userFileName, physicalFile, user);
    }

    /**
     * Change directory.
     */
    public boolean changeWorkingDirectory(String dir) {


        // not a directory - return false
        UsbFile physicalFile = UsbFtpFile.getPhysicalFile(rootDir, currDir, dir, caseInsensitive);
        if (physicalFile == null || !physicalFile.isDirectory()) {
            return false;
        }

        // strip user root and add last '/' if necessary
        dir = physicalFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() - 1);
        if (dir.charAt(dir.length() - 1) != '/') {
            dir = dir + '/';
        }

        currDir = dir;
        return true;
    }

    /**
     * Is the file content random accessible?
     */
    public boolean isRandomAccessible() {
        return true;
    }

    /**
     * Dispose file system view - does nothing.
     */
    public void dispose() {
    }
}
