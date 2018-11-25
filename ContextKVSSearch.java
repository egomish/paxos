import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class ContextKVSSearch extends ContextKVS implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());
    doKVSSearch(exch);
}

public void doKVSSearch (HttpExchange exch)
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
            resbody.isExists = false;
        } else {
            rescode = 200;
            resbody.result = "Success";
            resbody.isExists = true;
        }
    } else {
        rescode = 405;
        resbody.result = "Error";
        resbody.msg = method + " " + path + " not allowed";
    }

    resbody.payload = this.getHistory();
    response = new HttpRes(rescode, resbody.toJSON());
    sendResponse(exch, response);
}


}
