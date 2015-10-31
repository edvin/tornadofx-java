package tornadofx;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public interface JsonModel {
	void updateModel(JsonObject json);
	void toJSON(JsonObjectBuilder builder);

	default <T extends JsonModel> T clone() {
		try {
			T clone = (T) getClass().newInstance();
			JsonObjectBuilder builder = Json.createObjectBuilder();
			toJSON(builder);
			clone.updateModel(builder.build());
			return clone;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
