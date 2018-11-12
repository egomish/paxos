public class DockerTester {


public static void
main (String[] args)
{
    String[] nodes = {"localhost:4002", "localhost:4003"};
    HttpResponse response;
    HttpResponse[] responses;
    String reqbody;

/*
//broadcast hello world
    Client[] multi = Client.readyMulticast(nodes, "GET", "/hello", null);
    for (Client cl : multi) {
        cl.doSync();
        System.out.println(cl.getDestIP() + ": " + cl.getResponseBody());
    }
*/

/*
//on nodes[0], paxos/propose "this is a test"
    reqbody = "this is a test";
    Client cl = new Client(nodes[0], "POST", "/paxos/proposer", reqbody);
    cl.sendAsync();
    cl.receiveAsync();
    System.out.println(nodes[0] + ": " + cl.getResponseBody());
*/

//XXX: this test passes, but the proposals are not concurrent
//TODO: add threading to Client so async requests don't block until they finish
//on nodes[0] and nodes[1], concurrently paxos/propose "this is a test"
    String reqbody1 = "this is a test 1";
    String reqbody2 = "this is a test 2";
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", reqbody1);
    Client cl2 = new Client(nodes[1], "POST", "/paxos/proposer", reqbody2);
    cl1.sendAsync();
    cl2.sendAsync();
    cl1.receiveAsync();
    cl2.receiveAsync();
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());
    System.out.println(nodes[1] + ": " + cl2.getResponseBody());

}


}
