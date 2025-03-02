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
import java.util.Date;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.StepManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class SetErrorStatusCommand implements ICommandPlugin, IPlugin {

    private static final String ID = "setErrorStep";
    private static final String NAME = "setErrorStep Command Plugin";
    private HashMap<String, String> parameterMap;

    @Override
    public String getTitle() {
        return ID;
    }

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    public String getDescription() {
        return NAME;
    }

    public String getId() {
        return ID;
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
        }

        String stepId = this.parameterMap.get("stepId");
        try {
            Integer.parseInt(stepId);
        } catch (Exception e) {
            String title = "Wrong value";
            String message = "value for parameter 'stepId' is not a valid number.";
            return new CommandResponse(400, title, message);
        }

        return null;
    }

    @Override
    public CommandResponse execute() {

        Integer sourceid = Integer.parseInt(this.parameterMap.get("stepId"));

        try {
            Step source = StepManager.getStepById(sourceid);
            source.setBearbeitungsstatusEnum(StepStatus.ERROR);
            source.setEditTypeEnum(StepEditType.MANUAL_SINGLE);
            source.setBearbeitungszeitpunkt(new Date());
            source.setBearbeitungsbeginn(null);

            StepManager.saveStep(source);

        } catch (DAOException e) {
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            // return new CommandResponse(500,title, message);
            return new CommandResponse(title, message);
        }

        String title = "Command executed";
        String message = "Problem reported";
        return new CommandResponse(200, title, message);
        // return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command setErrorStep";
        String message = "This changes the status of a step to 'error'.";
        message += "\n - 'stepId' defines the task where the problem is noticed..";
        return new CommandResponse(200, title, message);
    }

    @Override
    public boolean usesHttpSession() {
        return false;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
    }

    @Override
    public void setHttpRequest(HttpServletRequest resp) {
    }

}
