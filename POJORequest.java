import com.google.gson.Gson;

//TODO: create a base class POJO with toJSON and fromJSON and extend from that
public class POJORequest extends ResponseBody {


public String getDestIP ()
{
    return ip;
}

public String getMethod ()
{
    return method;
}

public String getService ()
{
    return service;
}

public String getBody ()
{
    return body;
}

public void setDestIP (String destip)
{
    ip = destip;
}

//XXX: test this or remove it
//XXX: untested--what happens is the arg isn't JSON?
//XXX: when the arg isn't JSON, a JsonSyntaxException is throws and an empty ResponseBody object is returned
public static POJORequest fromJSON (String json)
{
    POJORequest body;
    try {
        Gson gson = new Gson();
        body =  gson.fromJson(json, POJORequest.class);
    } catch (Exception e) {
        System.out.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        body = new POJORequest();
    }
    return body;
}

public POJORequest ()
{
    ip = null;
    method = null;
    service = null;
    body = null;
}

public POJORequest (String i, String m, String s, String b)
{
    ip = i;
    method = m;
    service = s;
    body = b;
}


private String ip;
private String method;;
private String service;
private String body;

}
