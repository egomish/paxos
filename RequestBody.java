import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;

public class RequestBody {


public String get (String key)
{
    return kvPairs.get(key);
}

public String key ()
{
    return kvPairs.get("key");
}

public String value ()
{
    return kvPairs.get("value");
}
private String getBodyAsQuery (InputStream reqin)
{
    String body = "";
    try {
        int b = reqin.read();
        while (b != -1) {
            body += (char)b;
            b = reqin.read();
        }
    } catch (IOException e) {
        System.err.println("I/O exception occurred while reading request body.");
        body = null;
    }
    return body;
}

//XXX: all values are assumed to be type String
private String queryToJSON (String query)
{
    String json = "";
    json += "{";
    String[] pairs = query.split("&");
    for (int i = 0; i < pairs.length; i += 1) {
        if (i != 0) {
            json += ",";
        }
        String[] split = pairs[i].split("=");
        json += "'" + split[0] + "': '" + split[1] + "'";
    }
    json += "}";
    return json;
}

public RequestBody (InputStream reqin)
{
    Gson gson = new Gson();
    Type type = new TypeToken<HashMap<String, String>>(){}.getType();
    String query = getBodyAsQuery(reqin);
    String json = queryToJSON(query);
    kvPairs = gson.fromJson(json, type);
}


private HashMap<String, String> kvPairs;

}
