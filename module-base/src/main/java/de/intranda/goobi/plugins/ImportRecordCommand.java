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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.helper.JobCreation;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Prefs;

@PluginImplementation
public class ImportRecordCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(ImportRecordCommand.class);
    private static final String ID = "importRecordCommand";
    // private static final String NAME = "intranda Import Record Command Plugin";
    // private static final String VERSION = "1.0.20120716";
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
        return getTitle();
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
        if (!this.parameterMap.containsKey("filename")) {
            String title = "Missing parameter";
            String message = "No parameter 'filename' defined.";
            return new CommandResponse(400, title, message);
        }
        if (!this.parameterMap.containsKey("templateId")) {
            String title = "Missing parameter";
            String message = "No parameter 'templateId' defined.";
            return new CommandResponse(400, title, message);
        }

        return null;
    }

    @Override
    public CommandResponse execute() {
        //		Session session = HibernateUtilOld.getSessionFactory().openSession();
        //		if (!session.isOpen() || !session.isConnected()) {
        //			Connection con = ConnectionHelper.getConnection();
        //			session.reconnect(con);
        //		}
        try {
            Integer id = Integer.parseInt(this.parameterMap.get("templateId"));
            logger.debug("templateId is " + id);
            //			Prozess template = (Prozess) session.get(Prozess.class, id);
            Process template = ProcessManager.getProcessById(id);
            if (template != null) {
                logger.debug("template is " + template.getTitel());
            } else {
                logger.debug("cannot find template with id " + id);
            }
            // ProzessDAO dao = new ProzessDAO();
            // Prozess template = dao.get(id);

            String filename = parameterMap.get("filename");

            IImportPlugin wmi = (IImportPlugin) PluginLoader.getPluginByTitle(PluginType.Import, "goobiImport");
            if (wmi != null) {
                logger.debug("plugin " + wmi.getTitle() + " loaded");
            } else {
                logger.debug("cannot load plugin with title goobiImport");
            }
            // IImportPlugin wmi = (IImportPlugin) PluginLoader.getPlugin(PluginType.Import, "IntrandaGoobiImport");

            List<String> filenameList = new ArrayList<String>();
            filenameList.add(filename);
            List<Record> recordList = new ArrayList<Record>();
            List<ImportObject> answer = new ArrayList<ImportObject>();
            Prefs prefs = template.getRegelsatz().getPreferences();
            logger.debug("ruleset is " + template.getRegelsatz().getDatei());
            String tempfolder = ConfigurationHelper.getInstance().getTemporaryFolder();
            logger.debug("tempfolder is : " + tempfolder);
            wmi.setImportFolder(tempfolder);
            wmi.setPrefs(prefs);
            recordList = wmi.generateRecordsFromFilenames(filenameList);
            logger.debug("extracted " + recordList.size() + " records from " + filenameList.size() + " files.");
            answer = wmi.generateFiles(recordList);
            logger.debug("generated " + answer.size() + " metadata files");
            if (answer != null && answer.size() > 0) {
                ImportObject io = answer.get(0);
                if (!io.getImportReturnValue().equals(ImportReturnValue.ExportFinished)) {
                    String title = "Error during execution";
                    String message =
                            "import failed for " + io.getProcessTitle() + ", process generation failed: " + io.getImportReturnValue().getValue();
                    return new CommandResponse(500, title, message);
                }
                Process p = JobCreation.generateProcess(io, template);

                if (p == null) {
                    String title = "Error during execution";
                    String message = "import failed for " + io.getProcessTitle() + ", process generation failed";
                    return new CommandResponse(500, title, message);
                } else {
                    wmi.deleteFiles(filenameList);
                    String title = "Command executed";
                    String message = ImportReturnValue.ExportFinished.getValue() + " for " + io.getProcessTitle() + ", the Goobi id is "
                            + String.valueOf(p.getId());
                    return new CommandResponse(title, message);
                }
            } else {
                String title = "Error during execution";
                String message = "import failed for " + filename + ", process generation failed";
                return new CommandResponse(500, title, message);
            }
        } catch (Exception e) {
            logger.error(e);
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            return new CommandResponse(500, title, message);
            //		} finally {
            //			session.close();
        }
    }

    @Override
    public CommandResponse help() {
        String title = "Command importRecordCommand.";
        String message = "This command imports new records into goobi using file import mechanism of the ImportPlugin 'goobiImport'.";
        message += "\n - 'filename' defines the filename of the metadata file.";
        message += "\n - 'templateId' defines the id of the template to generate a process based on it.";
        return new CommandResponse(200, title, message);
    }
}
