public class Context {


public static HttpRes errorResponse (POJOReq request)
{
    String resmsg = request.reqMethod + " " + request.reqURL + " not allowed";
    HttpRes response = new HttpRes(405, resmsg);
    response.contentType = null;
    return response;
}

public HttpRes handle (POJOReq request)
{
    return HttpRes.clientError();
}

public Context (SmallServer ss)
{
    theServer = ss;
}

private SmallServer theServer;


}
