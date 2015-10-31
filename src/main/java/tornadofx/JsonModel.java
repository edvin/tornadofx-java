package tornadofx;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public interface JsonModel {
	/**
	 * Fetch JSON values and update the model properties
	 * @param json The json to extract values from
	 */
	void updateModel(JsonObject json);

	/**
	 * Build a JSON representation of the model properties
	 * @param builder A builder that should be filled with the model properties
	 */
	void toJSON(JsonObjectBuilder builder);

	/**
	 * Copy all properties from this object to the given target object by converting to JSON and then updating the target.
	 * @param target The target object to update with the properties of this model
	 */
	default void copy(JsonModel target) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		toJSON(builder);
		target.updateModel(builder.build());
	}

	/**
	 * Clone this model object by creating a new object of the same type and copy over all the model properties.
	 *
	 * @param <T> The type of object
	 * @return A new object of type T with the model properties of this object
	 */
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
