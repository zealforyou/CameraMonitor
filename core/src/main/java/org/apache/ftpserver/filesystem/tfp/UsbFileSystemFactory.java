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

package org.apache.ftpserver.filesystem.tfp;

import org.apache.ftpserver.filesystem.tfp.impl.UsbFileSystemView;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * Native file system factory. It uses the OS file system.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class UsbFileSystemFactory implements FileSystemFactory {

    private final Logger LOG = LoggerFactory
            .getLogger(UsbFileSystemFactory.class);

    private boolean createHome;

    private boolean caseInsensitive;

    /**
     * Should the home directories be created automatically
     * @return true if the file system will create the home directory if not available
     */
    public boolean isCreateHome() {
        return createHome;
    }

    /**
     * Set if the home directories be created automatically
     * @param createHome true if the file system will create the home directory if not available
     */

    public void setCreateHome(boolean createHome) {
        this.createHome = createHome;
    }

    /**
     * Is this file system case insensitive. 
     * Enabling might cause problems when working against case-sensitive file systems, like on Linux
     * @return true if this file system is case insensitive
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Should this file system be case insensitive. 
     * Enabling might cause problems when working against case-sensitive file systems, like on Linux
     * @param caseInsensitive true if this file system should be case insensitive
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Create the appropriate user file system view.
     */
    public FileSystemView createFileSystemView(User user) throws FtpException {
        synchronized (user) {
            // create home if does not exist
            if (createHome) {
                UsbFile homeDir = user.getUsbFileHome();
                if (homeDir != null && !homeDir.isDirectory()) {
                    LOG.warn("Not a directory :: " + homeDir.getAbsolutePath());
                    throw new FtpException("Not a directory :: " + homeDir.getAbsolutePath());
                }
                if (homeDir == null) {
                    LOG.warn("UsbFile is Null");
                    throw new FtpException("UsbFile is Null");
                }
            }

            FileSystemView fsView = new UsbFileSystemView(user,
                    caseInsensitive);
            return fsView;
        }
    }

}
