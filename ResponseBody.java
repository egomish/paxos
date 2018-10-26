import com.google.gson.Gson;

public class ResponseBody {


public static ResponseBody serverError ()
{
    ResponseBody body = new ResponseBody();
    body.setResult(false, "service is not available");
    return body;
}

public static ResponseBody clientError ()
{
    ResponseBody body = new ResponseBody();
    body.setResult(false, "unable to process request");
    return body;
}

public void setResult (boolean succ, String msg)
{
    success = succ;
    info = msg;
}

public void setSuccessFlag (boolean flag)
{
    success = flag;
}

public void setInfoString (String str)
{
    info = str;
}

public void setDebugString (String str)
{
    debug = str;
}


public void setReplacedFlag (int n)
{
    replaced = n;
}

public void setMessageString (String str)
{
    msg = str;
}

public void setValueString (String str)
{
    value = str;
}

public void setErrorString (String str)
{
    error = str;
}

public void setExistFlag (String str)
{
    isExist = str;
}


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

//XXX: test this or remove it
//XXX: untested--what happens is the arg isn't JSON?
//XXX: when the arg isn't JSON, a JsonSyntaxException is throws and an empty ResponseBody object is returned
public static ResponseBody fromJSON (String json)
{
    System.out.println("parsing JSON: '" + json + "'.");
    ResponseBody body = new ResponseBody();
    try {
        Gson gson = new Gson();
        body =  gson.fromJson(json, body.getClass());
    } catch (Exception e) {
        System.out.println("JSON exception: " + e.getMessage());
        body = ResponseBody.serverError();
    }
    return body;
}

public ResponseBody ()
{
    success = null;
    info = null;
    debug = null;

    replaced = null;
    msg = null;
    value = null;
    error = null;
    isExist = null;
}

public ResponseBody (boolean succ, String msg)
{
    success = succ;
    info = msg;
    debug = null;

    replaced = null;
    msg = null;
    value = null;
    error = null;
    isExist = null;
}

private Boolean success;
private String info;
private String debug;

private Integer replaced;
private String msg;
private String value;
private String error;
private String isExist;

}
