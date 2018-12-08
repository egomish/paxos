import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Pattern;

public class ContextKVS extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    POJOReq request = this.parseRequest(exch);
    request = this.consensusProtocol(request);
    request = this.shardProtocol(request, parseKeyFromURL(request.reqURL));
    HttpRes response;

    System.out.println("for shard: " + request.shardID);
    System.out.println("this shard: " + this.shardID);
    if ((request.shardID == null) || (request.shardID == this.shardID)) {
        response = doKVS(request);
    } else {
        response = new HttpRes(412, "not my shard");
        response.contentType = null;
    }
    sendResponse(exch, response);
}

public HttpRes doKVS (POJOReq request)
{
    String method = request.reqMethod;
    String url = request.reqURL;
    String urlkey = parseKeyFromURL(url);

    int rescode;
    POJOKVStore resbody = new POJOKVStore();
    HttpRes response;

    if (url.startsWith("/keyValue-store/search")) {
        response = doKVSSearch(request);
    }

    if (method.equals("GET")) {
        String value = kvStore.get(urlkey);
        if (value == null) {
            rescode = 404;
            resbody.result = "Error";
            resbody.msg = "Key does not exist";
        } else {
            rescode = 200;
            resbody.result = "Success";
            resbody.value = value;
        }
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        String reqvalue = parseVal(request.reqBody, request.charEncoding);
        if (!keyIsValid(urlkey)) {
            rescode = 422;
            resbody.result = "Error";
            resbody.msg = "Key not valid";
        } else if (!valueIsValid(reqvalue)) {
            rescode = 422;
            resbody.result = "Error";
            resbody.msg = "Value not valid";
        } else if (kvStore.get(urlkey) == null) {
            kvStore.put(urlkey, reqvalue);
            rescode = 200;
            resbody.replaced = false;
            resbody.msg = "Added successfully";
        } else {
            kvStore.put(urlkey, reqvalue);
            rescode = 201;
            resbody.replaced = true;
            resbody.msg = "Updated successfully";
        }
    } else if (method.equals("DELETE")) {
        String value = kvStore.get(urlkey);
        if (value != null) {
            kvStore.remove(urlkey);
            rescode = 200;
            resbody.result = "Success";
            resbody.msg = "Key deleted";
        } else {
            kvStore.remove(urlkey);
            rescode = 404;
            resbody.result = "Error";
            resbody.msg = "Key does not exist";
        }
    } else {
        rescode = 405;
        resbody.result = "Error";
        resbody.msg = method + " " + url + " not allowed";
    }

//    resbody.payload = this.getHistory();
    response = new HttpRes(rescode, resbody.toJSON());
    return response;
}

public HttpRes doKVSSearch (POJOReq request)
{
    String method = request.reqMethod;
    String url = request.reqURL;
    String urlkey = parseKeyFromURL(url);

    int rescode;
    POJOKVStore resbody = new POJOKVStore();
    HttpRes response;

    if (method.equals("GET")) {
        String value = kvStore.get(urlkey);
        if (value == null) {
            rescode = 404;
            resbody.result = "Error";
            resbody.msg = "Key does not exist";
            resbody.isExists = false;
        } else {
            rescode = 200;
            resbody.result = "Success";
            resbody.isExists = true;
        }
    } else {
        rescode = 405;
        resbody.result = "Error";
        resbody.msg = method + " " + url + " not allowed";
    }

//    resbody.payload = this.getHistory();
    response = new HttpRes(rescode, resbody.toJSON());
    return response;
}

//XXX: if key contains '/' characters, key will be parsed incorrectly
protected static String parseKeyFromURL (String url)
{
    String path = url.split(Pattern.quote("?"))[0]; //remove the query portion, if any
    String[] strarr = path.split("/");
    String key = strarr[strarr.length - 1]; //get the last token in the path
    return key;
}

//XXX: only handles requests with no content type or form-urlecoded
//     assumes that if there is an encoding, only the first one matters
protected static String parseVal (String reqbody, String encoding)
{
    POJOKeyVal pojo = POJOKeyVal.fromJSON(reqbody);
    String value = pojo.val;

    if (encoding != null) {
        if (encoding.equals("application/x-www-form-urlencoded")) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                System.err.println("bad encoding: " + e.getMessage());
            }
        } else {
            System.err.println("unknown encoding: " + encoding);
        }
    }

    return value;
}

protected static boolean keyIsValid (String key)
{
    if (key.length() == 0) {
        return false;
    }
    if (key.length() > 200) {
        return false;
    }
    for (int i = 0; i < key.length(); i += 1) {
        if ('a' <= key.charAt(i) && key.charAt(i) <= 'z') {
            continue;
        }
        if ('A' <= key.charAt(i) && key.charAt(i) <= 'Z') {
            continue;
        }
        if ('0' <= key.charAt(i) && key.charAt(i) <= '9') {
            continue;
        }
        if (key.charAt(i) == '_') {
            continue;
        }
        return false;
    }
    return true;
}

protected static boolean valueIsValid (String value)
{
    if (value == null) {
        return false;
    } else if (value.getBytes().length > 1000000) {
        return false;
    }
    return true;
}

protected ContextKVS ()
{
    kvStore = new HashMap<String, String>();
}


}
