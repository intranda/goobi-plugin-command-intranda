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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class AddPropertyCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(AddPropertyCommand.class);

    private static final String ID = "AddProperty";

    private HashMap<String, String> parameterMap;

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    @Override
    public String getTitle() {
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
    public CommandResponse validate() {
        if (!parameterMap.containsKey("processId")) {
            String title = "Missing parameter";
            String message = "No parameter 'processId' defined.";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey("property")) {
            String title = "Missing parameter";
            String message = "No parameter 'property' defined.";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey("value")) {
            String title = "Missing parameter";
            String message = "No parameter 'value' defined.";
            return new CommandResponse(400, title, message);
        }

        return null;
    }

    @Override
    public CommandResponse execute() {
        boolean overwriteExistingProperty = false;

        if (parameterMap.get("overwriteExistingProperty") != null && parameterMap.get("overwriteExistingProperty").equalsIgnoreCase("true")) {
            overwriteExistingProperty = true;
        }

        Integer processID = null;
        try {
            processID = Integer.parseInt(parameterMap.get("processId"));
        } catch (Exception e) {
            String title = "Wrong value.";
            String message = "The parameter 'processID' cannot be interpreted as a number.";
            return new CommandResponse(400, title, message);
        }
        String propertyName = parameterMap.get("property");
        String propertyValue = parameterMap.get("value");

        if (propertyName == null || propertyName.isEmpty()) {
            String title = "Wrong value.";
            String message = "The parameter 'property' is empty.";
            return new CommandResponse(400, title, message);
        }
        try {
            propertyName = URLDecoder.decode(propertyName, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            logger.warn("cannot encode " + propertyName);
        }

        try {
            propertyValue = URLDecoder.decode(propertyValue, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            logger.warn("cannot encode " + propertyValue);
        }

        Process process = null;

        process = ProcessManager.getProcessById(processID);

        boolean propertyExistsAlready = false;
        if (overwriteExistingProperty) {
            List<Processproperty> propertyList = process.getEigenschaften();
            for (Processproperty property : propertyList) {
                if (property.getTitel().equalsIgnoreCase(propertyName)) {
                    propertyExistsAlready = true;
                    property.setWert(propertyValue);
                    break;
                }
            }
        }
        if (!propertyExistsAlready) {
            Processproperty pe = new Processproperty();
            pe.setContainer(0);
            pe.setTitel(propertyName);
            pe.setWert(propertyValue);
            pe.setProzess(process);
            process.getEigenschaften().add(pe);
        }
        try {
            ProcessManager.saveProcess(process);
        } catch (DAOException e) {
            logger.error(e);
            String title = "Process cannot be saved.";
            String message = "Error during saving of process. See the logfile for more information.";
            return new CommandResponse(400, title, message);
        }
        String title = "Command executed";
        String message = "Created new property.";
        return new CommandResponse(200, title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command AddProperty";
        String message = "This command creates a property for processes.";
        message += "\n - 'processId' is mandatory. 'processId' defines the id of the process.";
        message += "\n - 'property' is mandatory. 'property' defines the name of the property.";
        message += "\n -  'value' is mandatory. The value defines the value of the property.";
        message +=
                "\n -  'overwriteExistingProperty' is optional. The value defines if the value of an existing property with the same name gets overwritten or if a new one gets created.";

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
