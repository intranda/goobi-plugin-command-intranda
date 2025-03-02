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
import java.io.File;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.intranda.goobi.archiving.Archiver;
import de.sub.goobi.export.download.ExportMets;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ArchiveProcessCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(ArchiveProcessCommand.class);

    private static final String dashes = "--------------------";
    private static File configFile;
    private static String defaultConfigFilePath = "archiving.properties";

    private static final String ID = "archiveProcess";
    //	private static final String NAME = "Archiving Command Plugin";
    //	private static final String VERSION = "1.0.20120518";

    private HashMap<String, String> parameterMap;

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
        return false;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
    }

    @Override
    public void setHttpRequest(HttpServletRequest req) {
    }

    @Override
    public CommandResponse validate() {
        if (!parameterMap.containsKey("processId")) {
            String title = "Missing parameter";
            String message = "No parameter 'processId' defined.";
            return new CommandResponse(400, title, message);
            //			return new CommandResponse(title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {

        //get config
        //		if(parameterMap.containsKey("config")) {
        //			String configPath = parameterMap.get("config");
        //			logger.debug("Found config path parameter " + configPath);
        //			if(configPath != null && !configPath.isEmpty() && !configPath.contentEquals("null")) {				
        //				configFile = new File(configPath);
        //			}
        //		}
        //check if we have a valid config path

        if (configFile == null || !configFile.isFile()) {
            Helper help = new Helper();
            configFile = new File(help.getGoobiConfigDirectory(), defaultConfigFilePath);
            if (!configFile.isFile()) {
                logger.error("Unable to locate config file. Aborting");
                String title = "Missing resources";
                String message = "Unable to locate config file";
                return new CommandResponse(500, title, message);
                //				return new CommandResponse(title, message);
            }
        }

        Process p = null;
        File processDir = null;
        File origImagesDir = null;
        //		Session session = HibernateUtilOld.getSessionFactory().openSession();
        //		if (!session.isOpen() || !session.isConnected()) {
        //			Connection con = ConnectionHelper.getConnection();
        //			session.reconnect(con);
        //		}
        try {
            //get Process data

            p = ProcessManager.getProcessById(Integer.valueOf(parameterMap.get("processId")));
            //			p = dao.get(Integer.valueOf(parameterMap.get("processId")));
            processDir = new File(p.getProcessDataDirectory());
            origImagesDir = new File(p.getImagesOrigDirectory(true));
        } catch (Exception e) {
            logger.error("Unable to find process");
            String title = "Process Error";
            String message = "Unable to find process";
            return new CommandResponse(500, title, message);
            //			return new CommandResponse(title, message);
        }
        //create exported mets
        try {
            File exportedMetsDir = new File(processDir, "exported_mets");
            logger.debug("attempting to create exported mets in " + exportedMetsDir.getAbsolutePath());
            exportedMetsDir.mkdirs();
            ExportMets exportMets = new ExportMets();
            exportMets.startExport(p, exportedMetsDir.getAbsolutePath() + "/");
        } catch (Exception e) {
            System.out.println(e.toString() + "\n" + e);
            logger.error("Unable to create exported Mets");
            String title = "Process Error";
            String message = "Unable to create exported Mets";
            return new CommandResponse(500, title, message);
            //			return new CommandResponse(title, message);
        }

        //start archiving
        Archiver archiver = new Archiver(configFile, origImagesDir, p.getTitel());
        try {
            logger.info(dashes + "Starting Goobi Archiver" + dashes);
            archiver.start();
            archiver.join();
        } catch (InterruptedException e) {
            logger.debug("Archiving interrupted");
        }
        logger.info(dashes + "Closing Goobi Archiver" + dashes);

        String title = "Done Archiving";
        String message = "Archive created: " + archiver.getArchive();
        return new CommandResponse(200, title, message);
        //		return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command archiveProcess";
        String message = "Syntax: \"command=archiveProcess\"; \"processId=[The processId of the Goobi process to be archived]\".";
        message = message.concat(
                "\nThis plugin requires the file \"archiving.properties\" in the goobi/config folder. There, additional parameters such as destination path can be adjusted.");
        return new CommandResponse(200, title, message);
        //		return new CommandResponse(title, message);
    }

}
