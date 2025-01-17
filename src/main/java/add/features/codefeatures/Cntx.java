package add.features.codefeatures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Cntx<I> {

	public final static String PREFIX = "CNTX";

	private Object identifier = null;

	private Map<String, I> information = new HashMap<>();

	public Cntx() {
		super();
	}

	public void put(CodeFeatures cntx, I a) {
		this.getInformation().put(cntx.name(), a);

	}

	public I get(CodeFeatures cntx) {
		return this.getInformation().get(cntx.name());

	}

	public Cntx(Object identifier, Map<String, I> information) {
		super();
		this.identifier = identifier;
		this.information = information;
	}

	public Cntx(Object identifier) {
		super();
		this.identifier = identifier;
	}


	public Map<String, I> getInformation() {
		return information;
	}

	public void setInformation(Map<String, I> information) {
		this.information = information;
	}

	@Override
	public String toString() {
		return "Cntx [" + information + "]";
	}

	@SuppressWarnings("unchecked")
	public JsonObject toJSON() {

		JsonObject generalStatsjson = new JsonObject();
		JSONParser parser = new JSONParser();
		for (String generalStat : information.keySet()) {
			Object vStat = information.get(generalStat);

			try {
				JsonElement value = calculateValue(parser, vStat);
				generalStatsjson.add(generalStat, value);
			} catch (Exception e) {
				System.out.println("Error property: " + generalStat);
				e.printStackTrace();
			}

		}

		return generalStatsjson;
	}

	@SuppressWarnings("unchecked")
	public JsonElement calculateValue(JSONParser parser, Object vStat) throws Exception {
		JsonElement value = null;
		if (vStat instanceof Cntx) {
			Cntx<Object> cntx = (Cntx) vStat;
			JsonObject composed = new JsonObject();
			for (String property : cntx.getInformation().keySet()) {
				JsonElement v = calculateValue(parser, cntx.getInformation().get(property));
				composed.add(property, v);
			}
			return composed;
		} else if (/* vStat instanceof AstorOutputStatus || */ vStat instanceof String) {
			JsonPrimitive p = new JsonPrimitive(JSONObject.escape(vStat.toString()));
			value = p;
		} else if (vStat instanceof Collection<?>) {
			JsonArray sublistJSon = new JsonArray();
			Collection acollec = (Collection) vStat;
			for (Iterator iterator = acollec.iterator(); iterator.hasNext();) {
				Object anItemList = (Object) iterator.next();
				sublistJSon.add(calculateValue(parser, anItemList));
			}
			value = sublistJSon;
		} else {
			try {

				JsonPrimitive p = new JsonPrimitive(JSONObject.escape(vStat.toString()));
				value = p;
			} catch (Exception e) {
				// System.out.println("Error");
			}
		}
		return value;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Object identifier) {
		this.identifier = identifier;
	}

}
