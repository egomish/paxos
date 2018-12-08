import com.google.gson.Gson;

public class POJOShard extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOShard fromJSON (String json)
{
    POJOShard pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOShard.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOShard();
    }
    return pojo;
}

public POJOShard ()
{
    this(null, null, null, null);
}

public POJOShard (Integer i, String s, String m, Integer c)
{
    id = i;
    shard_ids = s;
    members = m;
    count = c;
}


public Integer id;
public String shard_ids;
public String members;
public Integer count;

}
