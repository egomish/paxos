import java.util.HashMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextKVS extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String uri = exch.getRequestURI().toString();
    String query = exch.getRequestURI().getQuery();
    System.err.println(this.receiveLog(exch.getRequestMethod(), uri));

    POJOResHttp response;

    if ((query == null) || (!query.contains("fromhistory=true"))) {
        //get consensus to do the request
        POJOResHttp paxosres = doProposal(exch);
        if (paxosres.resCode == 200) {
            int resultindex;
            try {
                String info = POJOResBody.fromJSON(paxosres.resBody).info;
                resultindex = Integer.parseInt(info);
            } catch (NumberFormatException e) {
                //something really bad has happened if info isn't reqindex
                e.printStackTrace();
                throw e;
            }
            //do requests up to and including this one
            response = this.playHistoryTo(resultindex);
        } else {
            //something terrible happened while getting consensus
            throw new IllegalStateException();
        }

        //return the result to the client
        sendResponse(exch, response);
        return;
    }

    //query contains "fromhistory=true"--actually execute the request
    doKVS(exch);
}

//XXX: if key contains '/' characters, key will be parsed as just the 
//     characters following the final slash in the key
private static String parseKeyFromPath (String path)
{
    if (path == null) {
        return null;
    }
    String[] strarr = path.split("/");
    String key = strarr[strarr.length - 1];
    //make sure key is actually the whole key
    return key;
}

//XXX: only handles requests with no content type or form-urlecoded
//     assumes that if there is an encoding, only the first one matters
private static String parseValueFromRequest (HttpExchange exch)
{
    String body = Client.fromInputStream(exch.getRequestBody());
    POJOKeyValBody reqbody = POJOKeyValBody.fromJSON(body);
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

private static boolean keyIsValid (String key)
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

private static boolean valueIsValid (String value)
{
    if (value == null) {
        return false;
    } else if (value.getBytes().length > 1000000) {
        return false;
    }
    return true;
}

private void doKVS (HttpExchange exch)
{
    //request fields
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String urlkey = parseKeyFromPath(uri.getPath());

    //response fields
    int rescode = 200;
    POJOResBody resbody;

    String subpath = uri.getPath().substring(16); //subtract "/keyValue-store/"
    if (subpath.startsWith("search/")) {
        String subsubpath = subpath.substring(7); //subtract "search/"
        if (!method.equals("GET")) {
            rescode = 405;
            resbody = new POJOResBody(false, "method not allowed");
        } else {
            String value = kvStore.get(urlkey);
            if (value != null) {
                rescode = 200;
                resbody = new POJOResBody(true, "key exists");
                resbody.msg = "Success";
                resbody.isExist = "true";
            } else {
                rescode = 404;
                resbody = new POJOResBody(true, "key does not exist");
                resbody.msg = "Error";
                resbody.error = "Key does not exist";
                resbody.isExist = "false";
            }
        }
    } else {
        if (method.equals("GET")) {
            String value = kvStore.get(urlkey);
            if (value == null) {
                rescode = 404;
                String info = "key does not exist";
                resbody = new POJOResBody(false, info);
                resbody.msg = "Error";
                resbody.error = "Key does not exist";
            } else {
                resbody = new POJOResBody(true, value);
                resbody.msg = "Success";
                resbody.value = value;
            }
        } else if (method.equals("PUT") || (method.equals("POST"))) {
            String reqvalue = parseValueFromRequest(exch);
            if (!ContextKVS.keyIsValid(urlkey)) {
                rescode = 422;
                resbody = new POJOResBody(false, "key is invalid");
                resbody.msg = "Error";
                resbody.error = "Key not valid";
            } else if (!ContextKVS.valueIsValid(reqvalue)) {
                rescode = 422;
                String info = "value is invalid";
                resbody = new POJOResBody(false, info);
                resbody.msg = "Error";
                resbody.error = "Value is missing";
            } else if (kvStore.get(urlkey) == null) {
                kvStore.put(urlkey, reqvalue);
                rescode = 201;
                resbody = new POJOResBody(true, "key added");
                resbody.debug = reqvalue;
                resbody.replaced = 0;
                resbody.msg = "Added successfully";
            } else {
                kvStore.put(urlkey, reqvalue);
                rescode = 200;
                resbody = new POJOResBody(true, "key updated");
                resbody.msg = "Updated successfully";
                resbody.replaced = 1;
            }
        } else if (method.equals("DELETE")) {
            String value = kvStore.get(urlkey);
            if (value != null) {
                kvStore.remove(urlkey);
                rescode = 200;
                resbody = new POJOResBody(true, "key deleted");
                resbody.msg = "Success";
            } else {
                rescode = 404;
                String info = "key does not exist";
                resbody = new POJOResBody(false, info);
                resbody.msg = "Error";
                resbody.error = "Key does not exist";
            }
        } else {
            rescode = 405;
            String info = method + " " + uri + " not allowed";
            resbody = new POJOResBody(false, info);
        }
    }
    Client cl = new Client(ipAndPort, "GET", "/history", null);
    cl.doSync();
    resbody.payload = cl.getResponse().resBody;
    String resmsg = resbody.toJSON();
    sendResponse(exch, rescode, resmsg);
}

protected ContextKVS()
{
    String mainip = System.getenv().get("MAINIP");
    if (mainip == null) {
        kvStore = new HashMap<String, String>();
    } else {
        kvStore = null;
    }
}


public static HashMap<String, String> kvStore;

}
