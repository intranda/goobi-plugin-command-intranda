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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ViewerUploadCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(ViewerUploadCommand.class);

    private static final String COMMAND_TITLE = "viewer_update";
    private static final String PARAMETER_PROCESS = "process";
    private static final String PARAMETER_FOLDERNAME = "foldername";
    private static final String PARAMETER_FILENAME = "filename";

    private HashMap<String, String> parameterMap;
    private HttpServletRequest request;

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    @Override
    public String getTitle() {
        return COMMAND_TITLE;
    }

    public String getDescription() {
        return COMMAND_TITLE;
    }

    @Override
    public void setParameterMap(HashMap<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public CommandResponse validate() {
        if (!parameterMap.containsKey(PARAMETER_PROCESS)) {
            String title = "Missing parameter";
            String message = "No parameter 'process' defined";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey(PARAMETER_FOLDERNAME)) {
            String title = "Missing parameter";
            String message = "No parameter 'foldername' defined";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey(PARAMETER_FILENAME)) {
            String title = "Missing parameter";
            String message = "No parameter 'filename' defined";
            return new CommandResponse(400, title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {
        String identifier = parameterMap.get(PARAMETER_PROCESS);
        String foldername = parameterMap.get(PARAMETER_FOLDERNAME);
        String filename = parameterMap.get(PARAMETER_FILENAME);

        logger.debug("Import data for process with title " + identifier);

        List<Integer> processIdList = MetadataManager.getProcessesWithMetadata("CatalogIDDigital", identifier);
        Integer processId = null;
        if (processIdList.isEmpty()) {
            String title = "SEARCH ERROR";
            String value = "Found no process with id " + identifier;
            logger.error(value);
            return new CommandResponse(500, title, value);
        } else if (processIdList.size() > 1) {
            for (Integer id : processIdList) {
                List<StringPair> spl = MetadataManager.getMetadata(id);
                for (StringPair sp : spl) {
                    if (sp.getOne().trim().equals("CatalogIDDigital")) {
                        String value = sp.getTwo();
                        String[] parts = value.split(";");
                        for (String part : parts) {
                            if (part.equals(identifier)) {
                                processId = id;
                                break;
                            }
                        }
                    }
                }
            }
            //            String title = "SEARCH ERROR";
            //            String value = "Found more than one process with id " + identifier;
            //            logger.error(value);
            //            return new CommandResponse(500, title, value);
        } else if (processIdList.size() == 1) {
            processId = processIdList.get(0);
        }
        {
            String fileextension = foldername;
            if (fileextension.contains("_")) {
                fileextension = fileextension.substring(fileextension.lastIndexOf("_"));
            }

            Process process = ProcessManager.getProcessById(processId);
            File viewerFolder;
            try {
                File exportFolder = new File(process.getExportDirectory());
                viewerFolder = new File(exportFolder, process.getTitel() + fileextension);
                if (!viewerFolder.exists() && !viewerFolder.mkdirs()) {
                    String title = "IO Error";
                    String value = "Folder " + foldername + " can not be created.";
                    return new CommandResponse(500, title, value);
                }
            } catch (SwapException e) {
                String title = "IO Error";
                String value = "Folder " + foldername + " can not be created.";
                return new CommandResponse(500, title, value);
            } catch (IOException e) {
                String title = "IO Error";
                String value = "Folder " + foldername + " can not be created.";
                return new CommandResponse(500, title, value);
            }
            OutputStream out = null;
            File destination = new File(viewerFolder, filename);
            try {
                out = new FileOutputStream(destination);
                logger.debug("write to temporay file " + destination.getAbsolutePath());
                InputStream in = request.getInputStream();
                logger.debug("read request input stream");
                int numRead;
                byte[] buf = new byte[4096];
                while ((numRead = in.read(buf)) >= 0) {
                    out.write(buf, 0, numRead);
                }
                out.flush();

            } catch (Exception e) {
                logger.debug(e);
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
            }
        }
        return new CommandResponse("Import finished", "Import finished");
    }

    @Override
    public CommandResponse help() {
        String title = "Command viewer_update";
        String message = "This command accepts files from intranda viewer and save the data to a process.";
        message += "\n The parameter 'process' defines to which process the file belongs."
                + "\n The parameter 'foldername' defines the folder within the export folder where the file gets saved."
                + "\n The parameter 'filename' defines the name of the file." + "\n The file is sended via http post request.";
        return new CommandResponse(200, title, message);
    }

    @Override
    public boolean usesHttpSession() {
        return true;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
    }

    @Override
    public void setHttpRequest(HttpServletRequest request) {
        this.request = request;
    }
}
