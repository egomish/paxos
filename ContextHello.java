public class ContextHello extends Context {


public HttpRes handle (POJOReq request)
{
    HttpRes response;

    if (request.reqMethod.equals("GET")) {
        response = new HttpRes(200, "Hello world!");
        response.contentType = null;
    } else {
        response = this.errorResponse(request);
    }

    return response;
}

public ContextHello (SmallServer ss)
{
    super(ss);
}


}
