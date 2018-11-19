import com.google.gson.Gson;

public class POJOReqBody extends POJO {


//TODO: use Generics in base class
public static POJOReqBody fromJSON (String json)
{
    POJOReqBody pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOReqBody.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOReqBody();
    }
    return pojo;
}

public POJOReqBody ()
{
    this(null, null);
}

public POJOReqBody (String k, String v)
{
    key = k;
    val = v;
}

public String key;
public String val;

}
