import com.google.gson.Gson;

public class ResponseBody {


public static ResponseBody serverError ()
{
    ResponseBody body = new ResponseBody();
    body.setMessageString("error");
    body.setErrorString("service is not available");
    return body;
}

public static ResponseBody clientError ()
{
    ResponseBody body = new ResponseBody();
    body.setMessageString("error");
    body.setErrorString("unable to process request");
    return body;
}

public void setReplacedFlag (int n)
{
    replaced = new Integer(n);
}

public void setMessageString (String str)
{
    msg = str;
}

public void setErrorString (String str)
{
    error = str;
}

public void setValueString (String str)
{
    value = str;
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
    replaced = null;
    msg = null;
    error = null;
    value = null;
}

private Integer replaced;
private String msg;
private String error;
private String value;

}
