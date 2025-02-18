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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.persistence.managers.ProcessManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ProcessFilterCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(ProcessFilterCommand.class);
    private static final String ID = "processFilter";
    private static final String NAME = "Process Filter Command Plugin";
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
        if (!this.parameterMap.containsKey("filter")) {
            String title = "Missing parameter";
            String message = "No parameter 'filter' defined.";
            return new CommandResponse(400, title, message);
            //			return new CommandResponse(title, message);
        }
        return null;
    }

    @Override
    public CommandResponse execute() {
        String filter = this.parameterMap.get("filter");

        List<Integer> myIds = new ArrayList<>();
        if (filter == null || filter.trim().isEmpty()) {
            filter = "";
        }
        try {

            String sql = FilterHelper.criteriaBuilder(filter, false, null, null, null, true, false);
            myIds = ProcessManager.getIdsForFilter(sql);

            String answer = "";
            if (myIds.size() > 0) {
                Collections.sort(myIds);
                if (this.parameterMap.containsKey("separator")) {
                    String separator = this.parameterMap.get("separator");
                    for (Integer i : myIds) {
                        answer += i + separator;
                    }
                    if (answer.endsWith(separator)) {
                        answer = answer.substring(0, answer.length() - separator.length());
                    }
                } else {
                    for (Integer i : myIds) {

                        answer += i + "\n";
                    }
                }
            }

            OutputStream out = this.response.getOutputStream();
            out.write(answer.getBytes());
            out.flush();
        } catch (IOException e) {
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
        String title = "Command processFilter";
        String message = "This command filters for processes. The complete filter syntax of goobi can be used.";
        message += "\n - 'filter' is mandatory. 'filter' defines the search request.";
        message +=
                "\n -  'separator' is optional. The value defines the separator between each single result id. The default separator is 'new line'.";
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
}
