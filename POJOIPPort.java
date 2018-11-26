import com.google.gson.Gson;

public class POJOIPPort extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOIPPort fromJSON (String json)
{
    POJOIPPort pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOIPPort.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOIPPort();
    }
    return pojo;
}

public POJOIPPort ()
{
    this(null);
}

public POJOIPPort (String i)
{
    ip_port = i;
}


public String ip_port;

}
