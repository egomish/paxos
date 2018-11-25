import com.google.gson.Gson;

public class POJOKeyVal extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOKeyVal fromJSON (String json)
{
    POJOKeyVal pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOKeyVal.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOKeyVal();
    }
    return pojo;
}

public POJOKeyVal ()
{
    this(null, null);
}

public POJOKeyVal (String k, String v)
{
    key = k;
    val = v;
}


public String key;
public String val;

}
