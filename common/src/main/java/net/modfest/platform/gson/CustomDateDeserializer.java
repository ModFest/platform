package net.modfest.platform.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomDateDeserializer implements JsonDeserializer<Date> {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm:ss a");

    @Override
    public Date deserialize(JsonElement json,
                            Type typeOfT,
                            JsonDeserializationContext context) throws JsonParseException {
		try {
            String dateString = json.getAsString();
            return DateFormat.getDateInstance().parse(dateString);
        } catch (ParseException ignored) {
			// Platform currently serializes dates with gson's builtin
			// serialization. Unfortunately this serialization isn't stable
			// this is an attempt to fix the differences
            String dateString = json.getAsString();
            try {
				if (dateString.startsWith("Sep ")) {
					dateString = dateString.replaceFirst("Sep ", "Sept ");
				}
                return dateFormat.parse(dateString.replace(" ", " "));
            } catch (ParseException e) {
                throw new JsonParseException("Error parsing date: " + json.getAsString(), e);
            }
        }
    }
}
