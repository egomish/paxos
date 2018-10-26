import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
    return kvPairs.get("val");
}

private String getRequestBody (InputStream reqin)
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

//XXX: query format is assumed to be form-urlencoded
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
    String reqbody = getRequestBody(reqin);

    kvPairs = new HashMap<String, String>();
    if (reqbody.length() != 0) {
        try {
            kvPairs = gson.fromJson(reqbody, type);
        } catch (JsonSyntaxException e) {
            String json = queryToJSON(reqbody);
            try {
                kvPairs = gson.fromJson(json, type);
            } catch (JsonSyntaxException e2) {
                System.err.println("unknown request body syntax " + reqbody);
            }
        }
    }
}


private HashMap<String, String> kvPairs;

}
