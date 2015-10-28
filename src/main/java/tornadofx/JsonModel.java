package tornadofx;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public interface JsonModel {
	void updateModel(JsonObject json);
	void toJSON(JsonObjectBuilder builder);
}
