import com.google.gson.Gson;

public class ResponseBody {


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

public ResponseBody ()
{
    replaced = null;
    msg = null;
    error = null;
    value = null;
}

Integer replaced;
String msg;
String error;
String value;

}
