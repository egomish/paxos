import com.google.gson.Gson;

public class POJOHistory extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOHistory fromJSON (String json)
{
    POJOHistory pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOHistory.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOHistory();
    }
    return pojo;
}

public POJOHistory ()
{
    this(null);
}

public POJOHistory (POJOReq[] p)
{
    history = p;
}


public POJOReq[] history;

}
