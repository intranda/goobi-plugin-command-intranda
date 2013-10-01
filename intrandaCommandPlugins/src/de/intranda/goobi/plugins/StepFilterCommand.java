package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.persistence.managers.StepManager;

@PluginImplementation
public class StepFilterCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(StepFilterCommand.class);
    private static final String ID = "stepFilter";
    private static final String NAME = "StepFilter Command Plugin";
    private HttpServletResponse response;
    private HashMap<String, String> parameterMap;

    @Override
    public String getTitle() {
        return ID;
    }

    public String getId() {
        return ID;
    }

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    @Override
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
        boolean stepOpenOnly = false;
        String filter = this.parameterMap.get("filter");
        List<Integer> myIds = new ArrayList<Integer>();
        if (filter == null || filter.trim().isEmpty()) {
            filter = "";
        }

        String param = this.parameterMap.get("stepOpenOnly");
        if (param != null && !param.trim().isEmpty()) {
            if (param.equalsIgnoreCase("true")) {
                stepOpenOnly = true;
            }
        }

        try {
        	String sql = FilterHelper.criteriaBuilder(filter, false, stepOpenOnly, false, false, true, true);
            myIds = StepManager.getIDList(sql);

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
        String title = "Command stepFilter";
        String message = "This command filters for steps. The complete filter syntax of goobi can be used.";
        message += "\n - 'filter' is mandatory. 'filter' defines the search request.";
        message +=
                "\n - 'stepOpenOnly' is optional. Allowed values are 'true' and 'false', the default value is 'false' It defines if the result contains only open steps or all.";
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
