import com.google.gson.Gson;

public class POJOResHttp extends POJO {


//TODO: use Generics in base class
public static POJOResHttp fromJSON (String json)
{
    POJOResHttp pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOResHttp.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOResHttp();
    }
    return pojo;
}

public POJOResHttp ()
{
    this(null, null);
}

public POJOResHttp (Integer c, String b)
{
    resCode = c;
    resBody = b;
}

public Integer resCode;
public String resBody;

}