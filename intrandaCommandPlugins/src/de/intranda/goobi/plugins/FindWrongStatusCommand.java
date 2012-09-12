package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;

import de.sub.goobi.Persistence.HibernateUtilOld;
import de.sub.goobi.helper.Helper;

@PluginImplementation
public class FindWrongStatusCommand implements ICommandPlugin, IPlugin {


	private static final String ID = "findWrongStatus";
//	private static final String NAME = "FindWrongStatus Command Plugin";
	private HttpServletResponse response;

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

	@Override
	public String getDescription() {
		return ID;
	}

	@Override
	public void setParameterMap(HashMap<String, String> parameterMap) {
	}

	@Override
	public void setHttpResponse(HttpServletResponse resp) {
		this.response = resp;
	}

	@Override
	public void setHttpRequest(HttpServletRequest req) {
	}

	@Override
	public CommandResponse validate() {
		return null;
	}

	@Override
	public CommandResponse execute() {
		String queryString = "select distinct(Bearbeitungsstatus) as status, ProzesseID as id from schritte order by ProzesseID, Reihenfolge;";
		Session session = Helper.getHibernateSession();
		if (session == null || !session.isOpen() || !session.isConnected()) {
			HibernateUtilOld.rebuildSessionFactory();
			session=Helper.getHibernateSession();
		}
		SQLQuery query = session.createSQLQuery(queryString);
		query.addScalar("status", StandardBasicTypes.INTEGER);
		query.addScalar("id", StandardBasicTypes.INTEGER);
		@SuppressWarnings("rawtypes")
		List list = query.list();
		// List<Integer> bla = new ArrayList<Integer>();
		HashMap<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
		for (Object obj : list) {
			Object[] objArr = (Object[]) obj;
			Integer prozessId = (Integer) objArr[1];
			Integer reihenfolge = (Integer) objArr[0];
			if (map.containsKey(prozessId)) {
				List<Integer> bla = map.get(prozessId);
				bla.add(reihenfolge);
			} else {
				List<Integer> bla = new ArrayList<Integer>();
				bla.add(reihenfolge);
				map.put(prozessId, bla);
			}
		}
		String answer = "";
		for (Integer processId : map.keySet()) {
			List<Integer> liste = map.get(processId);

			if (liste.size() > 0) {
				boolean zero = false;
				boolean one = false;
				boolean two = false;
				@SuppressWarnings("unused")
				boolean three = false;

				for (Integer order : liste) {
					if (order == 0) {
						zero = true;
					} else if (order == 1) {
						one = true;
					} else if (order == 2) {
						two = true;
					} else if (order == 3) {
						three = true;
					}
				}
				if (zero && !one && ! two) {
					answer += String.valueOf(processId) + ",";
				}
			}
		}
		try {
			if (answer.endsWith(",")) {
				answer = answer.substring(0, answer.length()-1);
			}
			OutputStream out = this.response.getOutputStream();
			out.write(answer.getBytes());
			out.flush();
		} catch (IOException e) {
		
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

}
