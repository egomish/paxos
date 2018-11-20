import com.google.gson.Gson;

public class POJOViewHistory extends POJO {


//TODO: use Generics in base class
public static POJOViewHistory fromJSON (String json)
{
    POJOViewHistory pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOViewHistory.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOViewHistory();
    }
    return pojo;
}

public POJOViewHistory ()
{
    this(null, null);
}

public POJOViewHistory (String v, String h)
{
    view = v;
    history = h;
}

public String view;
public String history;

}
