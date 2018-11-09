public class DockerTester {


public static void
main (String[] args)
{
    String[] nodes = {"localhost:4001", "localhost:4002"};
    HttpResponse[] responses;

//broadcast hello world
    responses = ClientRequest.sendBroadcastRequest(nodes, 
                                                   "GET", "/hello", null, 
                                                   null);
    for (int i = 0; i < responses.length; i += 1) {
        HttpResponse response = responses[i];
        System.out.println(nodes[i] + ": " + responses[i].getResponseBody());
    }

//broadcast paxos/proposer
    responses = ClientRequest.sendBroadcastRequest(nodes, 
                                                   "GET", "/paxos/proposer", null, 
                                                   null);
    for (int i = 0; i < responses.length; i += 1) {
        HttpResponse response = responses[i];
        System.out.println(nodes[i] + ": " + responses[i].getResponseBody());
    }
}


}
