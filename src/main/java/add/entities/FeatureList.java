package add.entities;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import add.main.Config;
import add.main.Constants;

public class FeatureList {

    private List<Feature> featureList;
    private Config config;

    public FeatureList(Config config) {
        this.config = config;
        this.featureList = new ArrayList<>();
    }

    public void add(Feature feature) {
        this.featureList.add(feature);
    }

    public String toCSV() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < featureList.size(); i++) {
            Feature feature = featureList.get(i);
            for (String featureName : feature.getFeatureNames()) {
                output.append(featureName + Constants.CSV_SEPARATOR);
            }
        }
        output.append(Constants.LINE_BREAK);
        for (int i = 0; i < featureList.size(); i++) {
            Feature feature = featureList.get(i);
            for (String featureName : feature.getFeatureNames()) {
                int counter = feature.getFeatureCounter(featureName);
                output.append(counter + Constants.CSV_SEPARATOR);
            }
        }
        return output.toString();
    }

    public List<Feature> getFeatureList(){
        return featureList;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        for (Feature feature : featureList) {
            for (String featureName : feature.getFeatureNames()) {
                o.addProperty(featureName, feature.getFeatureCounter(featureName));
            }
        }
        return o;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(toJson()) + "\n";
    }

}
