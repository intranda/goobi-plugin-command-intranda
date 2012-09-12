package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.IEvaluableFilter;
import org.goobi.production.flow.statistics.hibernate.UserDefinedFilter;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import de.sub.goobi.Beans.Prozess;

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
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		String filter = this.parameterMap.get("filter");
		
		List<Integer> myIds = new ArrayList<Integer>();
		if (filter == null || filter.trim().isEmpty()) {
			filter = "";
		}
		try {
		IEvaluableFilter myFilteredDataSource = new UserDefinedFilter(filter);
		Criteria crit = myFilteredDataSource.getCriteria();
		crit.add(Restrictions.eq("istTemplate", Boolean.valueOf(false)));
		for (@SuppressWarnings("unchecked")
		Iterator<Object> it = crit.setFirstResult(0).setMaxResults(Integer.MAX_VALUE).list().iterator(); it.hasNext();) {
			Prozess p = (Prozess) it.next();
			myIds.add(p.getId());
		}

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
			return new CommandResponse(500,title, message);
//			return new CommandResponse(title, message);
		}
		String title = "Command executed";
		String message = "";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(200,title, message);
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
