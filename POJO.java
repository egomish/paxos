import com.google.gson.Gson;

public class POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

//TODO: use Generics so child classes can use directly and class can be abstract
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


}
