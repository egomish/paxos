import java.util.HashMap;

public class ContextKVS extends Context {


public HttpRes handle (POJOReq request)
{
    HttpRes response;

    String method = request.reqMethod;
    String url = request.reqURL;
    String body = request.reqBody;

    if (method.equals("GET")) {
        System.out.println("GET!!");
        response = new HttpRes(501, "not implemented");
        response.contentType = null;
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        System.out.println("POST!!");
        response = new HttpRes(501, "not implemented");
        response.contentType = null;
    } else if (method.equals("DELETE")) {
        System.out.println("DELETE!!");
        response = new HttpRes(501, "not implemented");
        response.contentType = null;
    } else {
        response = this.errorResponse(request);
    }

    return response;
}

public ContextKVS (SmallServer ss)
{
    super(ss);
    kvStore = new HashMap<String, String>();
}

private HashMap<String, String> kvStore;

}
