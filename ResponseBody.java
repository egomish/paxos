import com.google.gson.Gson;

public class ResponseBody {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

ResponseBody (int success, String m, String err)
{
    replaced = success;
    msg = m;
    error = err;
}

int replaced;
String msg;
String error;

}
