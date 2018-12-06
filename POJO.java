import com.google.gson.Gson;

public class POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJO fromJSON (String json)
{
    POJO pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJO.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJO();
    }
    return pojo;
}

public POJO ()
{
    this(null, null);
}

public POJO (String r, String m)
{
    result = r;
    msg = m;
}


public String result;
public String msg;

}
