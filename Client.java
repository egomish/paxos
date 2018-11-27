import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Client implements Runnable {


/* 
 *  From Runnable interface.
 */
public void run ()
{
    sendAsync();
    recvAsync();
    messageComplete = true;
}

public static String fromInputStream (InputStream in)
{
    String str = "";

    try {
        int b = in.read();
        while (b != -1) {
            str += (char)b;
            b = in.read();
        }
    } catch (IOException e) {
        System.err.println("I/O exception: " + e.getMessage());
        str = "";
    }

    if (str.length() == 0) {
        str = null;
    }

    return str;
}

public void fireAsync ()
{
    messageComplete = false;
    messageThread = new Thread(this);
    messageThread.start();
}

public boolean done ()
{
    if (messageComplete) {
        try {
            messageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    return messageComplete;
}

public void doSync ()
{
    this.sendAsync();
    this.recvAsync();
}

private void sendAsync ()
{
    //set URL
    String urlstr = clientReq.destIP + clientReq.reqURL;
    try {
        destURL = new URL("http://" + urlstr);
    } catch (MalformedURLException e) {
        System.err.println("[" + urlstr + "] malformed url: " + e.getMessage());
        httpRes = HttpRes.clientError();
        return;
    }

    //open connection
    try {
        httpConn = (HttpURLConnection)destURL.openConnection();
    } catch (IOException e) {
        System.err.println("[" + destURL + "] could not connect: " + e.getMessage());
        httpRes = HttpRes.clientError();
        return;
    }

    try {
        httpConn.setRequestMethod(clientReq.reqMethod);
    } catch (ProtocolException e) {
        System.err.println("[" + destURL + "] bad method: " + e.getMessage());
        httpRes = HttpRes.clientError();
        return;
    }

    //set request body
    try {
        if (clientReq.reqBody != null) {
            httpConn.setDoOutput(true);
            OutputStream out = httpConn.getOutputStream();
            out.write(clientReq.reqBody.getBytes());
            out.flush();
            out.close();
        }
    } catch (IOException e) {
        System.err.println("[" + clientReq.destIP  + "] could not create body: " + e.getMessage());
        httpRes = HttpRes.serverError();
        return;
    }

    //send request
    try {
        httpConn.connect();
    } catch (ConnectException e) {
        System.err.println("[" + clientReq.destIP + "] is down: " + e.getMessage());
        httpRes = HttpRes.serverError();
        httpRes.resCode = 501;
        return;
    } catch (NoRouteToHostException e) {
        System.err.println("[" + clientReq.destIP + "] not reachable: " + e.getMessage());
        httpRes = HttpRes.serverError();
        httpRes.resCode = 501;
        return;
    } catch (IOException e) {
        System.err.println("I/O exception while sending request.");
        e.printStackTrace();
        httpRes = HttpRes.serverError();
        return;
    }
}

private void recvAsync ()
{
    httpRes = new HttpRes();

    //get response
    try {
        httpRes.resCode = httpConn.getResponseCode();
    } catch (IOException e) {
        System.err.println("[" + clientReq.destIP + "] bad rescode: " + e.getMessage());
        httpRes = HttpRes.serverError();
        return;
    }

    InputStream in = null;
    try {
        if ((200 <= httpRes.resCode) && (httpRes.resCode < 300)) {
            in = httpConn.getInputStream();
        } else {
            in = httpConn.getErrorStream();
        }
    } catch (IOException e) {
        System.err.println("[" + clientReq.destIP + "] bad resbody: " + e.getMessage());
        httpRes = HttpRes.serverError();
        return;
    }

    //store response
    httpRes.resBody = Client.fromInputStream(in);
    if (httpRes.resBody == null) {
        System.err.println("[" + clientReq.destIP + "] failed to read response body.");
        httpRes = HttpRes.serverError();
        return;
    }
}

public HttpRes getResponse ()
{
    return httpRes;
}

public Client (POJOReq req)
{
    clientReq = req;
    destURL = null;
    httpConn = null;
    httpRes = null;

    messageComplete = true;
}


private POJOReq clientReq;
private URL destURL;
private HttpURLConnection httpConn;
private HttpRes httpRes;

private Thread messageThread;
private boolean messageComplete;

}
