package de.intranda.goobi.plugins;

/**
 * This file is part of a plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.File;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class UccUploadCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(UccUploadCommand.class);

    private static final String ID = "ucc_upload";
    // private static final String NAME = "UCC Upload Command Plugin";
    // private static final String VERSION = "1.0.20111109";

    private HashMap<String, String> parameterMap;
    private HttpServletRequest request;

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    @Override
    public String getTitle() {
        return ID;
    }

    public String getId() {
        return ID;
    }

    public String getDescription() {
        return ID;
    }

    @Override
    public void setParameterMap(HashMap<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public boolean usesHttpSession() {
        return true;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
    }

    @Override
    public void setHttpRequest(HttpServletRequest req) {
        request = req;
    }

    @Override
    public CommandResponse validate() {

        if (!parameterMap.containsKey("processId")) {
            String title = "Missing parameter";
            String message = "No parameter 'processId' defined";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        }

        return null;
    }

    @Override
    public CommandResponse execute() {
        logger.debug("Execute UccUpload");
        Integer processId = Integer.parseInt(parameterMap.get("processId"));
        logger.debug("process id is " + processId);
        logger.debug("get hibernate session");
        java.io.File archive = new java.io.File(ConfigurationHelper.getInstance().getTemporaryFolder(), processId + ".zip");
        logger.debug("created temporary file " + archive.getAbsolutePath());
        if (archive.exists()) {
            logger.debug("File " + archive.getAbsolutePath() + " does exist already. Try to delete it.");
            System.gc();
            boolean deleted = archive.delete();
            if (deleted) {
                logger.debug("File " + archive.getAbsolutePath() + " deleted.");
            } else {
                logger.error("File " + archive.getAbsolutePath() + " could not be deleted.");
            }
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            out = new java.io.FileOutputStream(archive);
            logger.debug("write to temporay file");
            in = request.getInputStream();
            logger.debug("read request input stream");
            int numRead;
            byte[] buf = new byte[4096];
            while ((numRead = in.read(buf)) >= 0) {
                out.write(buf, 0, numRead);
            }
            out.flush();
            logger.debug("finished data import");
            Process process = ProcessManager.getProcessById(processId);

            logger.debug("loaded process " + process.getTitel());
            File metaDest = new File(process.getMetadataFilePath());
            metaDest.renameTo(new File(process.getMetadataFilePath() + ".ucc." + System.currentTimeMillis()));
            logger.debug("metadata file is " + metaDest.getAbsolutePath());
            File anchorDest = new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml"));

            File metaSource = new File(archive + "/meta.xml");
            File anchorSource = new File(archive + "/meta_anchor.xml");
            metaSource.copyTo(metaDest);
            logger.debug("metadata file is overwritten");
            if (anchorSource.exists()) {
                logger.debug("anchor file exist");
                anchorDest.renameTo(new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml.ucc." + System.currentTimeMillis())));
                anchorSource.copyTo(new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml")));
                logger.debug("anchor file is overwritten");
            }
        } catch (Exception e) {
            logger.error(e);
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            return new CommandResponse(500, title, message);
            //          return new CommandResponse(title, message);
        } finally {

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }

        String title = "Command executed";
        String message = "Message to process log added";
        return new CommandResponse(200, title, message);
        //      return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command ucc_upload";
        String message = "this command accepts a ucc sip file for a specific process.";
        message += "\n The parameter 'processId' defines to which process the sip file belongs. The sip file is sended via http post request.";
        // return new CommandResponse(200, title, message);
        return new CommandResponse(title, message);
    }
}
