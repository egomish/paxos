import com.google.gson.Gson;

public class POJOReq extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOReq fromJSON (String json)
{
    POJOReq pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOReq.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOReq();
    }
    return pojo;
}

public boolean isEmpty ()
{
    if (destIP != null) {
        return false;
    }
    if (reqMethod != null) {
        return false;
    }
    if (reqURL != null) {
        return false;
    }
    if (reqBody != null) {
        return false;
    }
    return true;
}

public String toString ()
{
    String str = "";
    str += "{";
    if (!this.isEmpty()) {
        str += reqURL;
        str += " (" + shardID + ")";
    }
    str += "}";
    return str;
}

public POJOReq ()
{
    this(null, null, null, null, null);
}

public POJOReq (String i, String m, String u, String b)
{
    this(i, m, u, b, null);
}

public POJOReq (String i, String m, String u, String b, Integer s)
{
    destIP = i;
    reqMethod = m;
    reqURL = u;
    reqBody = b;
    shardID = s;
}


public String destIP;
public String reqMethod;
public String reqURL;
public String reqBody;
public Integer shardID;

}
