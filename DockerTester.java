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
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl.getResponseBody());
*/

/*
//on nodes[0] and nodes[1], concurrently paxos/propose "this is a test"
    String reqbody1 = "this is a test 1";
    String reqbody2 = "this is a test 2";
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", reqbody1);
    Client cl2 = new Client(nodes[1], "POST", "/paxos/proposer", reqbody2);
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());
    System.out.println(nodes[1] + ": " + cl2.getResponseBody());
*/

/*
//on nodes[0], paxos/propose three different messages in sequence
    String[] messages = {"first message", "second messsage", "third messsage"};
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", messages[0]);
    cl1.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());

    Client cl2 = new Client(nodes[0], "POST", "/paxos/proposer", messages[1]);
    cl2.fireAsync();
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl2.getResponseBody());

    Client cl3 = new Client(nodes[0], "POST", "/paxos/proposer", messages[2]);
    cl3.fireAsync();
    while (!cl3.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl3.getResponseBody());
*/

/*
//on nodes[0], paxos/propose three different concurrent messages in sequence
    String[] messages = {"first message", "second messsage", "third messsage"};
    String[] messages2 = {"foo", "bar", "baz"};
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", messages[0]);
    Client cl2 = new Client(nodes[1], "POST", "/paxos/proposer", messages2[0]);
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());
    System.out.println(nodes[1] + ": " + cl2.getResponseBody());

    cl1 = new Client(nodes[0], "POST", "/paxos/proposer", messages[1]);
    cl2 = new Client(nodes[1], "POST", "/paxos/proposer", messages2[1]);
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());
    System.out.println(nodes[1] + ": " + cl2.getResponseBody());

    cl1 = new Client(nodes[0], "POST", "/paxos/proposer", messages[2]);
    cl2 = new Client(nodes[1], "POST", "/paxos/proposer", messages2[2]);
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponseBody());
    System.out.println(nodes[1] + ": " + cl2.getResponseBody());
*/

//on nodes[0], paxos/propose an API call: "PUT /keyValue-store/foo {val: 'bar'}"
    String json = "{"
                + "method: " + "'" + "PUT" + "'" + ", "
                + "service: " + "'" + "/keyValue-store/foo" + "'" + ", "
                + "body: " + "'" + "{val: 'bar'}" + "'" + ", "
                + "}";
    reqbody = "this is a test";
    Client cl = new Client(nodes[0], "POST", "/paxos/proposer", json);
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl.getResponseBody());

}


}
