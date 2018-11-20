import com.google.gson.Gson;

public class POJOResBody extends POJO {


public static POJOResBody clientError ()
{
    return new POJOResBody(false, "unable to process request");
}

public static POJOResBody serverError ()
{
    return new POJOResBody(false, "service is not available");
}

//TODO: use Generics in base class
public static POJOResBody fromJSON (String json)
{
    POJOResBody pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOResBody.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOResBody();
    }
    return pojo;
}

public POJOResBody ()
{
    this(false, null);
}

public POJOResBody (boolean s, String i)
{
    success = s;
    info = i;
    debug = null;

    replaced = null;
    msg = null;
    value = null;
    error = null;
    isExist = null;
    result = null;
}

public boolean success;
public String info;
public String debug;

//required to conform to HW2 spec
public Integer replaced;
public String msg;
public String value;
public String error;
public String isExist;

//required to conform to HW3 spec
public String result;

}
