public class DockerTester {


public static void
main (String[] args)
{
    String[] nodes = {"localhost:4002", "localhost:4005"};

/*
//broadcast hello world
    Client[] multi = Client.readyMulticast(nodes, "GET", "/hello", null);
    for (Client cl : multi) {
        cl.doSync();
        System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
    }
*/

/*
//on nodes[0], paxos/propose: "{
//                                index: 0, //HERE
//                                method: 'PUT', 
//                                service: '/keyValue-store/foo', 
//                                body: "{val: 'bar'"}'
//                            }"
    String reqbody = "{method: 'PUT', service: '/keyValue-store/foo', body: \"{val: 'bar'}\"}";
    Client cl = new Client(nodes[0], "POST", "/paxos/proposer", reqbody);
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl.getResponse().resBody);
*/

/*
//on nodes[0] and nodes[1], concurrently paxos/propose
    String reqbody1 = "{method: 'PUT', service: '/keyValue-store/key4002', body: \"{val: 'test4002'}\"}";
    String reqbody2 = "{method: 'PUT', service: '/keyValue-store/key4003', body: \"{val: 'test4003'}\"}";
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
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody);
*/

/*
//on nodes[0], paxos/propose three different messages in sequence
    String[] messages = new String[3];
    messages[0] = "{method: 'PUT', service: '/keyValue-store/seq1', body: \"{val: 'value1'}\"}";
    messages[1] = "{method: 'PUT', service: '/keyValue-store/seq2', body: \"{val: 'value2'}\"}";
    messages[2] = "{method: 'PUT', service: '/keyValue-store/seq3', body: \"{val: 'value3'}\"}";
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", messages[0]);
    cl1.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);

    Client cl2 = new Client(nodes[0], "POST", "/paxos/proposer", messages[1]);
    cl2.fireAsync();
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl2.getResponse().resBody);

    Client cl3 = new Client(nodes[0], "POST", "/paxos/proposer", messages[2]);
    cl3.fireAsync();
    while (!cl3.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(nodes[0] + ": " + cl3.getResponse().resBody);
*/

/*
//on nodes[0], paxos/propose three different concurrent messages in sequence
    String[] msgs1 = new String[3];
    msgs1[0] = "{method: 'PUT', service: '/keyValue-store/key1', body: \"{val: 'value1'}\"}";
    msgs1[1] = "{method: 'GET', service: '/keyValue-store/key1', body: null}";
    msgs1[2] = "{method: 'DELETE', service: '/keyValue-store/key1', body: null}";
    String[] msgs2 = new String[3];
    msgs2[0] = "{method: 'PUT', service: '/keyValue-store/key2', body: \"{val: 'value2'}\"}";
    msgs2[1] = "{method: 'GET', service: '/keyValue-store/key2', body: null}";
    msgs2[2] = "{method: 'DELETE', service: '/keyValue-store/key2', body: null}";
    Client cl1 = new Client(nodes[0], "POST", "/paxos/proposer", msgs1[0]);
    Client cl2 = new Client(nodes[1], "POST", "/paxos/proposer", msgs2[0]);
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
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody.substring(0, 60));
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody.substring(0, 60));

    cl1 = new Client(nodes[0], "POST", "/paxos/proposer", msgs1[1]);
    cl2 = new Client(nodes[1], "POST", "/paxos/proposer", msgs2[1]);
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
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody.substring(0, 60));
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody.substring(0, 60));

    cl1 = new Client(nodes[0], "POST", "/paxos/proposer", msgs1[2]);
    cl2 = new Client(nodes[1], "POST", "/paxos/proposer", msgs2[2]);
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
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody.substring(0, 60));
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody.substring(0, 60));
*/

/*
//on nodes[0], paxos/propose an API call: "PUT /keyValue-store/foo {val: 'bar'}"
    String json = "{"
                + "method: " + "'" + "PUT" + "'" + ", "
                + "service: " + "'" + "/keyValue-store/foo" + "'" + ", "
                + "body: " + "'" + "{val: 'bar'}" + "'" + ", "
                + "}";
    String reqbody = "this is a test";
    Client cl = new Client(nodes[0], "POST", "/paxos/proposer", json);
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl.getResponse().resBody);
*/

/*
//on nodes[0], foo=bar
    Client cl = new Client(nodes[0], "POST", "/keyValue-store/foo", "{val: 'bar'}");
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl.getResponse().resBody);
*/

/**/
//propose a message concurrently on two nodes
    Client cl1 = new Client(nodes[0], "POST", "/keyValue-store/key1", "{val: 'val1'}");
    Client cl2 = new Client(nodes[1], "POST", "/keyValue-store/key2", "{val: 'val2'}");
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody);
/**/

/*
//propose three sequential messages concurrently on two nodes
    Client cl1 = new Client(nodes[0], "POST", "/keyValue-store/key1.first", "{val: 'val1'}");
    Client cl2 = new Client(nodes[1], "POST", "/keyValue-store/key2.first", "{val: 'val2'}");
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody);

    cl1 = new Client(nodes[0], "POST", "/keyValue-store/key1.second", "{val: 'val1'}");
    cl2 = new Client(nodes[1], "POST", "/keyValue-store/key2.second", "{val: 'val2'}");
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody);

    cl1 = new Client(nodes[0], "POST", "/keyValue-store/key1.third", "{val: 'val1'}");
    cl2 = new Client(nodes[1], "POST", "/keyValue-store/key2.third", "{val: 'val2'}");
    cl1.fireAsync();
    cl2.fireAsync();
    while (!cl1.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    while (!cl2.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(nodes[0] + ": " + cl1.getResponse().resBody);
    System.out.println(nodes[1] + ": " + cl2.getResponse().resBody);
*/

}


}
