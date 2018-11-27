import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class ContextKVS extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    String query = exch.getRequestURI().getQuery();

    HttpRes response;

    if ((query != null) && (query.contains("fromhistory=true"))) {
        //the request is from the server's history--execute it
        response = doKVS(exch);
    } else {
        //add the request to the server's history, then execute the request
        int reqindex = this.getConsensus(exch);
        if (reqindex == -1) {
            response = HttpRes.serverError();
        } else {
            response = this.playHistoryTo(reqindex);
        }
    }

    sendResponse(exch, response);
}

public HttpRes doKVS (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();
    String urlkey = parseKeyFromPath(path);

    int rescode;
    POJOKVStore resbody = new POJOKVStore();
    HttpRes response;

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
        String reqvalue = parseValueFromRequest(exch);
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
        resbody.msg = method + " " + path + " not allowed";
    }

    resbody.payload = this.getHistory();
    response = new HttpRes(rescode, resbody.toJSON());
    return response;
}

//XXX: if key contains '/' characters, key will be parsed incorrectly
protected static String parseKeyFromPath (String path)
{
    if (path == null) {
        return null;
    }
    String[] strarr = path.split("/");
    String key = strarr[strarr.length - 1];
    return key;
}

//XXX: only handles requests with no content type or form-urlecoded
//     assumes that if there is an encoding, only the first one matters
protected static String parseValueFromRequest (HttpExchange exch)
{
    String body = Client.fromInputStream(exch.getRequestBody());
    POJOKeyVal reqbody = POJOKeyVal.fromJSON(body);
    String value = reqbody.val;

    String contenttype = exch.getRequestHeaders().getFirst("Content-type");
    if (contenttype != null) {
        if (contenttype.equals("application/x-www-form-urlencoded")) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                System.err.println("bad encoding: " + e.getMessage());
            }
        } else {
            System.err.println("unknown encoding: " + contenttype);
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


public static HashMap<String, String> kvStore;

}
