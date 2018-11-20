public class DockerTester {


public static String[] view = {"10.0.0.2:4002", 
                               "10.0.0.3:4003", "10.0.0.4:4004", 
//                               "10.0.0.5:4005", "10.0.0.6:4006", 
//                               "10.0.0.7:4007", "10.0.0.8:4008"
                               };

//broadcast hello world
public static void test_hello ()
{
    Client[] multi = Client.readyMulticast(view, "GET", "/hello", null);
    for (Client cl : multi) {
        cl.doSync();
        System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
    }
}

//on view[0], foo=bar
public static void test_put_key (String node, String key, String val)
{
    Client cl = new Client(node, "POST", "/keyValue-store/" + key, "{val: '" + val + "'}");
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(node + ": " + cl.getResponse().resBody);
}

//propose messages concurrently
public static void test_concurrent_put (String[] nodes, String[] keys, String[] vals) {
    Client[] clients = new Client[keys.length];
    for (int i = 0; i < clients.length; i += 1) {
        Client cl = new Client(nodes[i], "POST", "/keyValue-store/" + keys[i], "{val: '" + vals[i] + "'}");
        clients[i] = cl;
        cl.fireAsync();
    }

    while (!Client.done(clients)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }

    for (int i = 0; i < clients.length; i += 1) {
        System.out.println(nodes[i] + ": " + clients[i].getResponse().resBody);
    }
}

//get current view
public static void test_get_view ()
{
    Client cl = new Client(view[0], "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

//add node to view
public static void test_add_view (String ipport)
{
    Client cl = new Client(view[0], "PUT", "/view", "{ip_port: '" + ipport + "'}");
    cl.doSync();

    cl = new Client(view[0], "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

//delete node from view
public static void test_delete_view (String ipport)
{
    Client cl = new Client(view[0], "DELETE", "/view", "{ip_port: '" + ipport + "'}");
    cl.doSync();

    cl = new Client(view[0], "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

public static void test_put_add_put ()
{
    test_put_key("10.0.0.2:4002", "foo", "bar");
    test_add_view("10.0.0.9:4009");
    test_put_key("10.0.0.9:4009", "abc", "value");
}

public static void
main (String[] args)
{
    String[] testnodes = {view[0], view[2]};

//    test_hello();
//    test_put_key(view[0], "foo", "bar");
//    test_concurrent_put(nodes, {"key1", "key2"}, {"val1", "val2"});
/*
    test_concurrent_put(testnodes, {"key1.1st", "key2.1st"}, 
                                   {"Quetzacotl", "Shiva"});
    test_concurrent_put(testnodes, {"key1.2nd", "key2.2nd"}, 
                                   {"Ifrit", "Siren"});
    test_concurrent_put(testnodes, {"key1.3rd", "key2.3rd"}, 
                                   {"Brothers", "Diablo"});
*/
//    test_get_view();
//    test_add_view("10.0.0.9:4009");
//    test_delete_view(view[1]);

    test_put_add_put();

}


}
