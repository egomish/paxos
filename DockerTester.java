public class DockerTester {


public static void
main (String[] args)
{
    String[] nodes = {"localhost:4002", "localhost:4003"};
    HttpResponse[] responses;

/*
//broadcast hello world
    responses = ClientRequest.sendBroadcastRequest(nodes, 
                                                   "GET", "/hello", null, 
                                                   null);
    for (HttpResponse res : responses) {
        System.out.println(res.getResponseBody());
    }
*/

//attempt paxos/proposer
    String reqbody = "this is a test";
    HttpResponse response = ClientRequest.sendRequest(nodes[0], 
                                          "GET", "/paxos/proposer", null, 
                                          reqbody);
    System.out.println(nodes[0] + ": " + response.getResponseBody());
}


}
