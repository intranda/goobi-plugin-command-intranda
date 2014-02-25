package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.goobi.beans.Process;
import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.MySQLHelper;
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

    @Override
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
        String processtitle = parameterMap.get(PARAMETER_PROCESS);
        String foldername = parameterMap.get(PARAMETER_FOLDERNAME);
        String filename = parameterMap.get(PARAMETER_FILENAME);

        logger.debug("Import data for process with title " + processtitle);

        List<Process> processList =
                ProcessManager.getProcesses("prozesse.titel", " prozesse.titel like '%" + MySQLHelper.escapeString(processtitle) + "%'");
        boolean secondSearch = false;
        logger.debug("Found " + processList.size() + " processes");
        for (Process p : processList) {
            logger.debug(p.getTitel());
        }
        if (processList.isEmpty()) {
            logger.debug("Found no process with title " + processtitle + ", searching for property with that value.");
            secondSearch = true;
        } else if (processList.size() > 1) {
            logger.debug("Found more than one process with title " + processtitle + ", searching for property with that value.");
            secondSearch = true;
        }
        if (secondSearch) {
            processList =
                    ProcessManager
                            .getProcesses(
                                    "prozesse.titel",
                                    "p.titel from prozesse p, werkstuecke w, werkstueckeeigenschaften we where p.ProzesseID = w.ProzesseID and w.werkstueckeID = we.werkstueckeID and we.WERT LIKE '%"
                                            + MySQLHelper.escapeString(processtitle) + "%'");
        }

        if (processList.isEmpty()) {
            String title = "SEARCH ERROR";
            String value = "Found no process with title " + processtitle;
            logger.error(value);
            return new CommandResponse(500, title, value);
        } else if (processList.size() > 1) {
            String title = "SEARCH ERROR";
            String value = "Found more than one process with title " + processtitle;
            logger.error(value);
            return new CommandResponse(500, title, value);
        } else {
            String fileextension = foldername;
            if (fileextension.contains("_")) {
                fileextension = fileextension.substring(fileextension.lastIndexOf("_"));
            }

            Process process = processList.get(0);
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
            } catch (DAOException e) {
                String title = "IO Error";
                String value = "Folder " + foldername + " can not be created.";
                return new CommandResponse(500, title, value);
            } catch (IOException e) {
                String title = "IO Error";
                String value = "Folder " + foldername + " can not be created.";
                return new CommandResponse(500, title, value);
            } catch (InterruptedException e) {
                String title = "IO Error";
                String value = "Folder " + foldername + " can not be created.";
                return new CommandResponse(500, title, value);
            }
            OutputStream out = null;
            File destination = new File(viewerFolder, filename);
            try {
                out = new FileOutputStream(destination);
                logger.debug("write to temporay file");
                InputStream in = request.getInputStream();
                logger.debug("read request input stream");
                int numRead;
                byte[] buf = new byte[4096];
                while ((numRead = in.read(buf)) >= 0) {
                    out.write(buf, 0, numRead);
                }
                out.flush();

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
            }
        }
        return null;
    }

    @Override
    public CommandResponse help() {
        String title = "Command viewer_upload";
        String message = "This command accepts files from intranda viewer and save the data to a process.";
        message +=
                "\n The parameter 'process' defines to which process the file belongs."
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
