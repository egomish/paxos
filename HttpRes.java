public class HttpRes {


public static HttpRes notAllowedError (String method, String path)
{
    HttpRes res = new HttpRes();
    res.resCode = 405;
    res.resBody = method + " " + path + " not allowed";
    res.contentType = null;
    return res;
}

public static HttpRes clientError ()
{
    HttpRes res = new HttpRes();
    res.resCode = 400;
    res.resBody = "unable to process request";
    res.contentType = null;
    return res;
}

public static HttpRes serverError ()
{
    HttpRes res = new HttpRes();
    res.resCode = 500;
    res.resBody = "service is unavailable";
    res.contentType = null;
    return res;
}

public String toString ()
{
    String str = "";
    str += "{";
    str += this.resCode + " ";
    str += "(" + this.contentType + "): ";
    str += this.resBody;
    str += "}";
    return str;
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
