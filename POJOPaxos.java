import com.google.gson.Gson;

public class POJOPaxos extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOPaxos fromJSON (String json)
{
    POJOPaxos pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOPaxos.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOPaxos();
    }
    return pojo;
}

public POJOPaxos ()
{
    this(null, null, null);
}

public POJOPaxos (Integer s, Integer r, POJOReq v)
{
    seqNum = s;
    reqIndex = r;
    accValue = v;
}


public Integer seqNum;
public Integer reqIndex;
public POJOReq accValue;

}
