package de.intranda.goobi.plugins;

import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.beans.Step;

import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;

import de.sub.goobi.persistence.managers.StepManager;

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
