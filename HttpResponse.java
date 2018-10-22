public class HttpResponse {


public int getResponseCode ()
{
    return rescode.intValue();
}

public String getResponseBody ()
{
    return resbody;
}

public void setResponseCode (int n)
{
    rescode = new Integer(n);
}

public void setResponseBody (String body)
{
    resbody = body;
}

public HttpResponse ()
{
    rescode = 0;
    resbody = null;
}


private Integer rescode;
private String resbody;

}
