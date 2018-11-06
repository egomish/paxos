public class DockerTester {


public static void
main (String[] args)
{
//broadcast hello world
    String[] nodes = {"localhost:4001", "localhost:4002", "localhost:4003"};
    HttpResponse[] responses;
    responses = ClientRequest.sendBroadcastRequest(nodes, 
                                                   "GET", "/hello", null, 
                                                   null);
    for (int i = 0; i < responses.length; i += 1) {
        HttpResponse response = responses[i];
        System.out.println(nodes[i] + ": " + responses[i].getResponseBody());
    }
}


}
