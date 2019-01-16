import com.google.gson.Gson;

public class POJONum extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJONum fromJSON (String json)
{
    POJONum pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJONum.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJONum();
    }
    return pojo;
}

public POJONum ()
{
    this(null);
}

public POJONum (Integer n)
{
    num = n;
}


public Integer num;;

}