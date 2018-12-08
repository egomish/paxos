import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ContextShard extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    POJOReq request = this.parseRequest(exch);
    request = this.consensusProtocol(request);
    HttpRes response;

    response = doShard(request);
    sendResponse(exch, response);
}

private HttpRes doShard (POJOReq request)
{
    String method = request.reqMethod;
    String path = request.urlPath;

    POJOShard resbody = new POJOShard();
    HttpRes response;

    if (path.startsWith("/shard/my_id")) {
        if (method.equals("GET")) {
            resbody.id = this.shardID;
            response = new HttpRes(200, resbody.toJSON());
        } else {
            response = HttpRes.notAllowedError(method, path);
        }
    } else if (path.startsWith("/shard/all_ids")) {
        if (method.equals("GET")) {
            resbody = new POJOShard();
            resbody.shard_ids = getShardView();
            response = new HttpRes(200, resbody.toJSON());
        } else {
            response = HttpRes.notAllowedError(method, path);
        }
    } else if (path.startsWith("/shard/members/")) {
        if (method.equals("GET")) {
            int shard = POJONum.fromJSON(request.reqBody).num;
            String allshards = this.getShardView();
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            response = HttpRes.notAllowedError(method, path);
        }
    } else if (path.startsWith("/shard/count/")) {
        if (method.equals("GET")) {
            String resmsg = method + " " + path + " not implemented";
            response = new HttpRes(501, resmsg);
            response.contentType = null;
        } else {
            response = HttpRes.notAllowedError(method, path);
        }
    } else if (path.startsWith("/shard/changeShardNumber")) {
        if ((method.equals("PUT")) || (method.equals("POST"))) {
            int shardnum = POJONum.fromJSON(request.reqBody).num;
            repartitionKeysAndNodes(shardnum);
            resbody = new POJOShard();
            resbody.shard_ids = getShardView();
            response = new HttpRes(200, resbody.toJSON());
        } else {
            response = HttpRes.notAllowedError(method, path);
        }
    } else {
        response = HttpRes.notAllowedError(method, path);
    }
    return response;
}

private int parseShardIDFromURL (String url)
{
    String path = url.split(Pattern.quote("?"))[0]; //remove the query portion, if any
    String[] strarr = path.split("/");
    String num = strarr[strarr.length - 1]; //get the last token in the path
    int numint;
    try {
        numint = Integer.valueOf(num);
    } catch (NumberFormatException e) {
        //it would be really weird if the shard id wasn't a number
        e.printStackTrace();
        numint = 0;
    }
    return numint;
}

private int parseShardNumFromURL (String url)
{
    String path = url.split(Pattern.quote("?"))[0]; //remove the query portion, if any
    String[] strarr = path.split("/");
    String num = strarr[strarr.length - 1]; //get the last token in the path
    int numint;
    try {
        numint = Integer.valueOf(num);
    } catch (NumberFormatException e) {
        //it would be really weird if the number of shards  wasn't a number
        e.printStackTrace();
        numint = 0;
    }
    return numint;
}

private void repartitionKeysAndNodes (int shardnum)
{
    int diff = shardnum - this.shardDir.size();

    //repartition keys
    if (diff < 0) {
        //removing shard(s)
        addShards(shardnum);
    } else if (diff > 0) {
        //adding shard(s)
        removeShards(shardnum);
    }

    //repartition nodes

    //replay history
    this.replayHistory();
}

private void addShards (int shardnum)
{
    //migrate keys to new shard
    for (int i = 0; i < shardnum - this.shardDir.size(); i += 1) {
        //create the new shard
        ArrayList<String> shard = new ArrayList<String>();
        int dense = this.getDensestShard();
        ArrayList<String> shardkeys = this.shardDir.get(dense);
        //migrate keys to the new shard
        for (int j = 0; j < shardkeys.size() / 2; j += 1) {
            //add shardDir[dense][j] to shardDir[new]
            //remove shardDir[dense][j] from shardDir[dense]
            shard.add(shardDir.get(dense).get(j));
            shardDir.get(dense).remove(j);
        }
        //add new shard to directory
        this.shardDir.add(shard);
    }
}

private void removeShards (int shardnum)
{
    //migrate keys from the removed shard
}

private String getShardView ()
{
    String str = "";
    for (int i = 0; i < this.shardDir.size(); i += 1) {
        if (shardDir.get(i) != null) {
            str += i + ",";
        }
    }
    str = str.substring(0, str.length() - 1); //strip trailing ','
    return str;
}

/*
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
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        //put key at smallestShard
        POJOReq request = new POJOReq(smallestShard, method, uri, reqbody);
        Client cl = new Client(request);
        cl.doSync();
        response = cl.getResponse();
        //verify smallest shard
        if ((200 <= response.resCode) && (response.resCode < 300)) {
        }
        smallestShard = getSmallestShard(response);
    } else if (method.equals("DELETE")) {
        //linear search for key, for now
        //remove key
        //verify smallest shard
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
        response = HttpRes.notAllowedError(method, path);
    }

    response.contentType = null;
    sendResponse(exch, response);
}
*/


}
