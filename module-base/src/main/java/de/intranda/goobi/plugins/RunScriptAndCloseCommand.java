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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.persistence.managers.StepManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class RunScriptAndCloseCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(RunScriptAndCloseCommand.class);
    private static final String ID = "runScriptAndClose";
    private static final String NAME = "RunScriptAndCloseCommand Command Plugin";
    private HttpServletResponse response;
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
        return NAME;
    }

    @Override
    public void setParameterMap(HashMap<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public CommandResponse validate() {
        if (!this.parameterMap.containsKey("stepId")) {
            String title = "Missing parameter";
            String message = "No parameter 'stepId' defined.";
            return new CommandResponse(400, title, message);
            //			return new CommandResponse(title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {
        String stepId = this.parameterMap.get("stepId");
        String scriptname = this.parameterMap.get("scriptname");
        try {
            int id = Integer.parseInt(stepId);
            Step step = StepManager.getStepById(id);
            //			Schritt step = new SchrittDAO().get(id);
            HelperSchritte hs = new HelperSchritte();

            if (scriptname != null && scriptname.length() > 0) {
                Map<String, String> scripts = step.getAllScripts();
                if (scripts.containsKey(scriptname)) {
                    String script = scripts.get(scriptname);

                    hs.executeScriptForStepObject(step, script, true);

                } else {
                    String title = "Error during execution";
                    String message = "script " + scriptname + " does not exist";
                    return new CommandResponse(500, title, message);
                    //					return new CommandResponse(title, message);
                }
            } else {

                hs.executeAllScriptsForStep(step, true);
            }
            String answer = "finished script";
            OutputStream out = this.response.getOutputStream();
            out.write(answer.getBytes());
            out.flush();
        } catch (Exception e) {
            logger.info(e);
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            return new CommandResponse(500, title, message);
            //			return new CommandResponse(title, message);
        }
        String title = "Command executed";
        String message = "";
        return new CommandResponse(200, title, message);
        //		return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command runScriptAndClose";
        String message = "This command calls scripts for a given task and closes the task too.";
        message += "\n - 'stepId' defines the id of the task.";
        message += "\n - 'scriptname' is optional and defines a script to call. If no script is defined, all scripts of the task gets started.";
        //		stepId
        //		scriptname
        return new CommandResponse(200, title, message);
        //		return new CommandResponse(title, message);
    }

    @Override
    public boolean usesHttpSession() {
        return true;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
        this.response = resp;
    }

    @Override
    public void setHttpRequest(HttpServletRequest resp) {
    }

    //	private void runScript(List<Prozess> inProzesse, String stepname, String scriptname) {
    //		HelperSchritteWithoutHibernate hs = new HelperSchritteWithoutHibernate();
    //		for (Prozess p : inProzesse) {
    //			for (Schritt step : p.getSchritteList()) {
    //				if (step.getTitel().equalsIgnoreCase(stepname)) {
    //					if (scriptname != null) {
    //						if (step.getAllScripts().containsKey(scriptname)) {
    //							String path = step.getAllScripts().get(scriptname);
    //							try {
    //								hs.executeScript(step, path, false);
    //							} catch (SwapException e) {
    //								Helper.setFehlerMeldung("Error while running script " + path, e);
    //							}
    //						}
    //					} else {
    //						try {
    //							hs.executeAllScriptsForStep(step, false);
    //						} catch (SwapException e) {
    //							Helper.setFehlerMeldung("Error while running scripts", e);
    //						} catch (DAOException e) {
    //							Helper.setFehlerMeldung("Error while running scripts", e);
    //						}
    //					}
    //				}
    //			}
    //		}
    //
    //	}
}
