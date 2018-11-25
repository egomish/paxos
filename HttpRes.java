public class HttpRes {


public static HttpRes clientError ()
{
    HttpRes res = new HttpRes();
    res.resCode = 400;
    res.resBody = "unable to process request";
    return res;
}

public static HttpRes serverError ()
{
    HttpRes res = new HttpRes();
    res.resCode = 500;
    res.resBody = "service is unavailable";
    return res;
}

public HttpRes ()
{
    this(0, null);
}

public HttpRes (int c, String b)
{
    resCode = c;
    contentType = "application/json";
    resBody = b;
}


public Integer resCode;
public String contentType;
public String resBody;

}
