import java.lang.reflect.Type;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class POJOHistory extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    Type type = new TypeToken<HashMap<Integer, String>>(){}.getType();
    String json = gson.toJson(this.history, type);
    return json;
}

//TODO: use Generics in base class
public static POJOHistory fromJSON (String json)
{
    POJOHistory pojo = new POJOHistory();
    try {
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<Integer, String>>(){}.getType();
        pojo.history = gson.fromJson(json, type);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOHistory();
    }
    return pojo;
}

public int size ()
{
    return history.size();
}

public int getNextIndex ()
{
    return history.size();
}

public POJOHistory ()
{
    history = new HashMap<Integer, String>();
}

public HashMap<Integer, String> history;

}
