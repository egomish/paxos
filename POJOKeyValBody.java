import com.google.gson.Gson;

public class POJOKeyValBody extends POJO {


//TODO: use Generics in base class
public static POJOKeyValBody fromJSON (String json)
{
    POJOKeyValBody pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOKeyValBody.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOKeyValBody();
    }
    return pojo;
}

public POJOKeyValBody ()
{
    this(null, null);
}

public POJOKeyValBody (String k, String v)
{
    key = k;
    val = v;
}

public String key;
public String val;

}
