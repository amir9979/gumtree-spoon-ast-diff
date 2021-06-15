package add.entities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import add.main.Config;
import add.main.Constants;

public abstract class Feature {

	private Config config;

	@SuppressWarnings("rawtypes")

	public void setConfig(Config config) {
		this.config = config;
	}

	public void incrementFeatureCounter(String key) {
		try {
			Field field = this.getClass().getDeclaredField(key);
			field.setAccessible(true);
			int value = (int) field.get(this);
			field.set(this, value + 1);

		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalArgumentException("Feature not found: " + key, e);
		}
	}

	public void setFeatureCounter(String key, int value) {
		try {
			Field field = this.getClass().getDeclaredField(key);
			field.setAccessible(true);
			field.set(this, value);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalArgumentException("Feature not found: " + key, e);
		}
	}

	public int getFeatureCounter(String key) {
		try {
			Field field = this.getClass().getDeclaredField(key);
			if (int.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				return (int) field.get(this);
			}
			throw new IllegalArgumentException("Feature not found" + key);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalArgumentException("Feature not found" + key, e);
		}
	}

	public List<String> getFeatureNames() {
		List<String> output = new ArrayList<>();
		Field[] declaredFields = this.getClass().getDeclaredFields();
		for (int i = 0; i < declaredFields.length; i++) {
			FeatureAnnotation annotation = declaredFields[i].getAnnotation(FeatureAnnotation.class);
			if (annotation != null) {
				output.add(annotation.key());
			}
		}
		return output;
	}

	public String toCSV() {
		StringBuilder output = new StringBuilder();
		for (String featureName : getFeatureNames()) {
			output.append(featureName + Constants.CSV_SEPARATOR);
		}
		output.append(Constants.LINE_BREAK);
		for (String featureName : getFeatureNames()) {
			int counter = getFeatureCounter(featureName);
			output.append(counter + Constants.CSV_SEPARATOR);
		}
		return output.toString();
	}

	public JsonObject toJson() {
		JsonObject JsonObjectFeatures = new JsonObject();
		for (String featureName : getFeatureNames()) {
			JsonObjectFeatures.addProperty(featureName, getFeatureCounter(featureName));
		}
//		JsonArray json = new JsonArray();
//		if (config != null) {
//			json.addProperty("commitId", this.config.getCurrentCommit());
//		}
//		json.addProperty(Character.toLowerCase(this.getClass().getSimpleName().charAt(0)) + this.getClass().getSimpleName().substring(1), JsonObjectFeatures);
		return JsonObjectFeatures;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(toJson()) + "\n";
	}

}
