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
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IValidatorPlugin;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.helper.ShellScript;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.metadaten.MetadatenVerifizierung;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class CloseStepByProcessIdCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(CloseStepByProcessIdCommand.class);

    private static final String ID = "closeStepByProcessId";
    //	private static final String NAME = "CloseStepByProcessId Command Plugin";
    //	private static final String VERSION = "1.0.20111109";

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
        if (!this.parameterMap.containsKey("processId")) {
            String title = "Missing parameter";
            String message = "No parameter 'processId' defined.";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {
        Integer id = Integer.parseInt(this.parameterMap.get("processId"));
        Process po = ProcessManager.getProcessById(id);
        List<Step> stepList = po.getSchritte();
        int countOpenSteps = 0;
        for (Step so : stepList) {
            if (so.getBearbeitungsstatusEnum().equals(StepStatus.OPEN) || so.getBearbeitungsstatusEnum().equals(StepStatus.INWORK)) {
                countOpenSteps++;
            }
        }
        if (countOpenSteps == 0) {
            // error, no open steps found
        } else if (countOpenSteps > 1) {
            // error, more than one open step found
        } else {
            for (Step so : stepList) {
                if (so.getBearbeitungsstatusEnum().equals(StepStatus.OPEN) || so.getBearbeitungsstatusEnum().equals(StepStatus.INWORK)) {
                    boolean valid = true;
                    String message = "Step not closed";
                    if (so.isTypMetadaten() && ConfigurationHelper.getInstance().isUseMetadataValidation()) {
                        MetadatenVerifizierung mv = new MetadatenVerifizierung();

                        mv.setAutoSave(true);
                        if (!mv.validate(po)) {
                            valid = false;
                            message = "Step not closed, metadata could not validated.";
                        }

                    }

                    /* Imagevalidierung */
                    if (so.isTypImagesSchreiben()) {
                        MetadatenImagesHelper mih = new MetadatenImagesHelper(null, null);
                        try {
                            if (!mih.checkIfImagesValid(po.getTitel(), po.getImagesOrigDirectory(false))) {
                                valid = false;
                                message = "Step not closed, images not found.";
                            }
                        } catch (Exception e) {
                            valid = false;
                            message = "Step not closed, images not found.";
                        }
                    }

                    if (so.getValidationPlugin() != null && so.getValidationPlugin().length() > 0) {
                        IValidatorPlugin ivp = (IValidatorPlugin) PluginLoader.getPluginByTitle(PluginType.Validation, so.getValidationPlugin());
                        ivp.setStep(so);
                        if (!ivp.validate()) {
                            valid = false;
                            message = "Step not closed, validation failed";
                        }
                    }

                    if (valid) {
                        // StepObject so = StepManager.getStepById(id);
                        logger.debug("loaded StepObject with id " + so.getId());
                        HelperSchritte hs = new HelperSchritte();
                        hs.CloseStepObjectAutomatic(so);

                        // try to remove symlink from user home 
                        if (parameterMap.get("username") != null && parameterMap.get("username").length() > 0) {
                            String homeDir = ConfigurationHelper.getInstance().getUserFolder();
                            String username = parameterMap.get("username");

                            String nach = homeDir + username + "/";

                            nach += po.getTitel() + " [" + po.getId() + "]";

                            /* Leerzeichen maskieren */
                            nach = nach.replaceAll(" ", "__");
                            File benutzerHome = new File(nach);

                            String command = ConfigurationHelper.getInstance().getScriptDeleteSymLink();
                            if (!command.isEmpty()) {
                                command += " " + benutzerHome;
                                // myLogger.debug(command);
                                try {
                                    ShellScript.legacyCallShell2(command, so.getProcessId());
                                } catch (java.io.IOException ioe) {
                                    logger.error("IOException UploadFromHome", ioe);
                                    String title = "Error during execution";
                                    message = "Step was closed, but unmount from user home failed";
                                    return new CommandResponse(500, title, message);
                                } catch (InterruptedException e) {
                                    logger.error("IOException UploadFromHome", e);
                                    String title = "Error during execution";
                                    message = "Step was closed, but unmount from user home failed";
                                    return new CommandResponse(500, title, message);
                                }
                            }
                        }

                        String title = "Command executed";
                        message = "Step closed";
                        return new CommandResponse(200, title, message);
                        // return new CommandResponse(title, message);
                    } else {
                        String title = "Error during execution";
                        //						String message = "Step not closed";
                        return new CommandResponse(500, title, message);
                        // return new CommandResponse(title, message);
                    }
                }
            }
        }
        String title = "Error during execution";
        String message = "Step not closed";
        return new CommandResponse(500, title, message);
        // return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command help for closeStepByProcessId";
        String message =
                "This command closes the first open step and opens the next task. If next task is an automatic task, all scripts gets started."
                        + "\n 'processId' defines the process where the first open step gets closed.";
        return new CommandResponse(200, title, message);
        // return new CommandResponse(title, message);
    }

}
