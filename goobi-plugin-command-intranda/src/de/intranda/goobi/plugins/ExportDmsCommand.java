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
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ExportDmsCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(ExportDmsCommand.class);

    private static final String ID = "exportDms";
    //	private static final String NAME = "ExportDMS Command Plugin";
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
        if (!parameterMap.containsKey("images")) {
            String title = "Missing parameter";
            String message = "No parameter 'images' defined.";
            return new CommandResponse(400, title, message);
            //			return new CommandResponse(title, message);
        }
        if (!parameterMap.containsKey("ocr")) {
            String title = "Missing parameter";
            String message = "No parameter 'ocr' defined.";
            return new CommandResponse(400, title, message);
            //			return new CommandResponse(title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {
        Integer id = Integer.parseInt(parameterMap.get("processId"));
        boolean images = Boolean.parseBoolean(parameterMap.get("images"));
        boolean ocr = Boolean.parseBoolean(parameterMap.get("ocr"));

        try {

            Process source = ProcessManager.getProcessById(id);
            if(source.getContainsExportStep()) {
	            //          ProzessDAO dao = new ProzessDAO();
	            //          Prozess p = dao.get(id);
	            ExportDms export = new ExportDms(images);
	            export.setExportFulltext(ocr);
	            if (export.startExport(source)) {
	                String title = "Command executed";
	                String message = "Process exported to DMS";
	                return new CommandResponse(200, title, message);
	            } else {
	                String title = "Error during execution";
	                String message = "An error occured.";
	                return new CommandResponse(500, title, message);
	            }
            }else {
            	String title = "Error during execution";
            	String message = "An error occured: no step with type Export found in process with id "+source.getId();
            	return new CommandResponse(500, title, message);
            }

        } catch (Exception e) {
            logger.error(e);
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            return new CommandResponse(500, title, message);
            //          return new CommandResponse(title, message);
        }

        //      return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command exportDms";
        String message = "This command triggers the export to DMS for a process.";
        message += " - 'processId' defines the ID of the process.";
        message += " - 'images' defines whether the images gets exported or not. Allowed values are 'true' and 'false'.";
        message += " - 'ocr' defines whether the ocr data gets exported or not. Allowed values are 'true' and 'false'.";
        return new CommandResponse(200, title, message);
        //		return new CommandResponse(title, message);
    }

}
