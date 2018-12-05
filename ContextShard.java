import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContextShard extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    HttpRes response;

    if (this.isOracle) {
        response = parseAndDoRequest(exch);
    } else {
        //send request to oracle
        System.out.println("shard forwarding not implemented!!");
        response = HttpRes.serverError();
    }
    sendResponse(exch, response);
}

private HttpRes parseAndDoRequest (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (path.startsWith("/shard/process")) {
        if ((method.equals("PUT")) || (method.equals("POST"))) {
            response = doShardableRequest(exch);
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else if (path.startsWith("shard/my_id")) {
        if (method.equals("GET")) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else if (path.startsWith("shard/all_ids")) {
        if (method.equals("GET")) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else if (path.startsWith("shard/members/")) {
        if (method.equals("GET")) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else if (path.startsWith("shard/count/")) {
        if (method.equals("GET")) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else if (path.startsWith("shard/changeShardNumber/")) {
        if ((method.equals("PUT")) || (method.equals("POST"))) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            String resmsg = method + " " + path + " not allowed";
            response = new HttpRes(405, resmsg);
            response.contentType = null;
        }
    } else {
        String resmsg = method + " " + path + " not allowed";
        response = new HttpRes(405, resmsg);
        response.contentType = null;
    }
}

private HttpRes doShardableRequest (HttpExchange exch)
{
    String reqstr = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = POJOReq.fromJSON(reqstr);

    //register request in history

    //to replicate, do request n times
        //get next shard
        String shardip = this.nextShard();

        //extract reqMethod, reqURL, reqBody
        String method = request.reqMethod;
        String url = request.reqURL;
        String body = request.reqBody;

        //put key on shard
        POJOReq shardreq = new POJOReq(shardip, method, url, body);
        Client cl = new Client(shardreq);
        cl.doSync();

    //return response from shard
    return cl.getResponse();
}

public void doShardableRequest_OLD (HttpExchange exch)
{
    String method = exch.getRequestMethod();

    if (method.equals("GET")) {
        //linear search for key, for now
    } else if ((method.equals("PUT") || (method.equals("POST"))) {
        //put key at smallestServer
        POJOReq request = new POJOReq(smallestServer, method, uri, reqbody);
        Client cl = new Client(request);
        cl.doSync();
        response = cl.getResponse();
        //verify smallest server
        if ((200 <= response.resCode) && (response.resCode < 300)) {
        }
        smallestServer = getSmallestServer(response);
    } else if ((method.equals("DELETE")) {
        //linear search for key, for now
        //remove key
        //verify smallest server
    } else {
        //return 405
    }






    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (method.equals("GET")) {
        String resmsg = "Hello world!";
        response = new HttpRes(200, resmsg);
    } else {
        String resmsg = method + " " + path + " not allowed";
        response = new HttpRes(405, resmsg);
    }

    response.contentType = null;
    sendResponse(exch, response);
}

protected ContextShard ()
{
    if (this.isOracle) {
        shardView = new ArrayList<SmallServer>();
        for (String node : this.getNodeView()) {
            shardView.add(node);
        }
        smallestProcess = shardView.at(0);
    } else {
        shardView = null;
        smallestProcess = null;
    }
    keyCount = 0;
    replicaCount = 0;
}


public static ArrayList<String> shardView;
public static String smallestProcess;
public static boolean isOracle;
public static int keyCount;
public static int replicaCount;

}
