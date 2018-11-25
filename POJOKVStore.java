import com.google.gson.Gson;

public class POJOKVStore extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOKVStore fromJSON (String json)
{
    POJOKVStore pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOKVStore.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOKVStore();
    }
    return pojo;
}

public POJOKVStore ()
{
    this(null, null, null, null);
}

public POJOKVStore (String v, Boolean r, Boolean e, POJOReq[] p)
{
    value = v;
    replaced = r;
    isExists = e;
    payload = p;
}


public String value;
public Boolean replaced;
public Boolean isExists;
public POJOReq[] payload;

}
