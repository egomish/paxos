public class DockerTester {


public static void
main (String[] args)
{
//broadcast hello world
    String[] nodes = {"10.0.0.41:8080", "10.0.0.42:8080"};
    HttpResponse[] responses;
    responses = ClientRequest.sendBroadcastRequest(nodes, 
                                                   "GET", "/hello", null, 
                                                   null);
    for (int i = 0; i < responses.length; i += 1) {
        HttpResponse response = responses[i];
        System.out.println(nodes[i] + ": " + responses[i].getResponseBody());
    }

//attempt paxos
    HttpResponse response;
    response = ClientRequest.sendRequest(nodes[0],
                                          "POST", "/paxos/proposer", null, 
                                          null);
    System.out.println(nodes[0] + ": " + response.getResponseBody());
}


}
