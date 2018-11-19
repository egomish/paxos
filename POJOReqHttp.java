import com.google.gson.Gson;

public class POJOReqHttp extends POJO {


//TODO: use Generics in base class
public static POJOReqHttp fromJSON (String json)
{
    POJOReqHttp pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOReqHttp.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOReqHttp();
    }
    return pojo;
}

public POJOReqHttp ()
{
    this(null, null, null, null, null);
}

public POJOReqHttp (String i, String m, String s, String b)
{
    ip = i;
    method = m;
    service = s;
    body = b;
    getConsensus = null;
}

public POJOReqHttp (String i, String m, String s, String b, Boolean c)
{
    ip = i;
    method = m;
    service = s;
    body = b;
    getConsensus = c;
}

public String ip;
public String method;
public String service;
public String body;
public Boolean getConsensus;

}
