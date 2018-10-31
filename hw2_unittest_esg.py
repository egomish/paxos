import unittest
import subprocess
import requests
import sys
import random
import time

hostname = 'localhost'  # Windows and Mac users can change this to the docker vm ip
contname = 'assignment2'  # Set your container name here
sudo = ''  # Make the value of this variable sudo if you need sudo to start containers


class TestHW2(unittest.TestCase):
    '''
    Creating a subnet:
        sudo docker network create --subnet=10.0.0.0/16 mynet
    '''
    ip_addresses = ['10.0.0.20', '10.0.0.21', '10.0.0.22']
    host_ports = ['8083', '8084', '8085']
    node_ids = []
    nodes_address = []
    has_been_run_once = False
    all_tests_done = False
    key1 = '_yJnRhMUqNHXlUvxTie9jfQ0n6DX2of2ET13aW1LGLnvF9ZBxowE5NluZ3bH0Ctlw65S6XjYftCIzIIWDRd8bS5ykWKZvZGVvQDvakcODw___yiN8purA8Xfl_9WOzCLGYyJF4K2q3yOaOnd6Iu9SEo'
    key2 = '6TLxbmwMTN4hX7L0QX5_NflWH0QKfrTlzcuM5PUQHS52___lCzKbEMxLZHhtfww3KcMoboDLjB6mw_wFfEz5v_TtHqvGOZnk4_8aqHga79BaHXzpU9_IRbdjYdQutAU0HEuji6Ny1Ol'
    key3 = '6TLxbmwMTN4hX7L0QX5_NflWH0QKfrTlzcuM5PUQHS52___lCizKbEMxLZHhtfww3KcMoboDLjB6mw_wFfEz5v_TtHqvGOZnk4_8aqHga79BaHXzpU9_IRbdjYdQutAU0HEuji6Ny1Ol_MSaBF4JdT0aiG_N7xAkoPH3AlmVqDN45KDGBz7_YHrLnbLEK11SQxZcKXbFomh9JpH_sbqXIaifqOy4g06Ab0q3WkNfVzx7H0hGhNlkINf5PF12'
    val1 = 'aaaasjdhvksadjfbakdjs'
    val2 = 'asjdhvksadjfbakdjs'
    val3 = 'bknsdfnbKSKJDBVKpnkjgbsnk'

    def spin_up_nodes(self):

        exec_string_main = sudo + " docker run -p 8083:8080 --net=mynet --ip=10.0.0.20 -d %s" % contname
        print(exec_string_main)
        self.__class__.node_ids.append(subprocess.check_output(exec_string_main, shell=True).strip('\n'))

        exec_string_forw1 = sudo + " docker run -p 8084:8080 --net=mynet -e MAINIP=10.0.0.20:8080 -d %s" % contname
        print(exec_string_forw1)
        self.__class__.node_ids.append(subprocess.check_output(exec_string_forw1, shell=True).strip('\n'))

        exec_string_forw2 = sudo + " docker run -p 8085:8080 --net=mynet -e MAINIP=10.0.0.20:8080 -d %s" % contname
        print(exec_string_forw2)
        self.__class__.node_ids.append(subprocess.check_output(exec_string_forw2, shell=True).strip('\n'))

        self.__class__.nodes_address = ['http://' + hostname + ":" + x for x in self.__class__.host_ports]
        #print(self.__class__.node_ids) #un-comment this line to display container IDs

    def setUp(self):

        if not self.__class__.has_been_run_once:
            self.__class__.has_been_run_once = True
            self.spin_up_nodes()
            print("Sleeping for 10 seconds to let servers bootup")
            time.sleep(10)

#### Part 1
# search for key that does not exist
    def test_a_search (self):
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/search/bad')
        d = res.json()
        self.assertEqual(res.status_code, 404)

# attempt to get key that does not exist
    def test_b_get (self):
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/bad')
        d = res.json()
        self.assertEqual(res.status_code, 404)

# attempt to delete nonexistent key
    def test_c_delete (self):
        res = requests.delete(self.__class__.nodes_address[0] + '/keyValue-store/bad')
        d = res.json()
        self.assertEqual(res.status_code, 404)

# put new key
    def test_d_put (self):
        res = requests.put(self.__class__.nodes_address[0] + '/keyValue-store/foo', data = {'val': 'bart'})
        d = res.json()
        self.assertEqual(res.status_code, 201)

# search for key that exists
    def test_e_search (self):
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/foo', data = {'val': 'bart'})
        d = res.json()
        self.assertEqual(res.status_code, 200)

# put (replace) existing key
    def test_f_replace (self):
        res = requests.put(self.__class__.nodes_address[0] + '/keyValue-store/foo', data = {'val': 'this is a new value'})
        d = res.json()
        self.assertEqual(res.status_code, 200)

# get key that exists
    def test_g_get (self):
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/foo')
        d = res.json()
        self.assertEqual(res.status_code, 200)

# delete existing key
    def test_h_delete (self):
        res = requests.delete(self.__class__.nodes_address[0] + '/keyValue-store/foo')
        d = res.json()
        self.assertEqual(res.status_code, 200)

# attempt to put invalid (too long) key
    def test_i_put (self):
        res = requests.put(self.__class__.nodes_address[0] + '/keyValue-store/' + self.__class__.key3, data={'val':self.__class__.val2})
        d = res.json()
        self.assertEqual(res.status_code, 422)

# attempt to put invalid (null) value
    def test_j_put (self):
        res = requests.put(self.__class__.nodes_address[0] + '/keyValue-store/badval', data = {})
        d = res.json()
        self.assertEqual(res.status_code, 422)

#### Part 2
# put key on primary, get key on proxy1
    def test_k_integration (self):
        res = requests.put(self.__class__.nodes_address[0] + '/keyValue-store/primary', data = {'val':self.__class__.val2})
        res = requests.get(self.__class__.nodes_address[1] + '/keyValue-store/primary')
        d = res.json()
        self.assertEqual(res.status_code, 200)

# put key on proxy1, get key on proxy2
    def test_m_integration (self):
        res = requests.put(self.__class__.nodes_address[1] + '/keyValue-store/proxy', data = {'val':self.__class__.val2})
        res = requests.get(self.__class__.nodes_address[2] + '/keyValue-store/proxy')
        d = res.json()
        self.assertEqual(res.status_code, 200)

# delete key on proxy1, get key on primary
    def test_n_integration (self):
        res = requests.delete(self.__class__.nodes_address[1] + '/keyValue-store/proxy')
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/proxy')
        d = res.json()
        self.assertEqual(res.status_code, 404)

# get key (from primary) when proxy is down
    def test_o_integration (self):
        res = requests.put(self.__class__.nodes_address[2] + '/keyValue-store/' + self.__class__.key1, data = {'val':self.__class__.val1})
        shell_command = "docker stop " + str(self.__class__.node_ids[2])
        subprocess.check_output(shell_command, shell=True)
        res = requests.get(self.__class__.nodes_address[0] + '/keyValue-store/' + self.__class__.key1)
        d = res.json()
        self.assertEqual(res.status_code, 200)

# attempt to get key (from proxy1) when primary is down
    def test_p_integration (self):
        res = requests.put(self.__class__.nodes_address[1] + '/keyValue-store/' + self.__class__.key1, data = {'val':self.__class__.val1})
        shell_command = "docker stop " + str(self.__class__.node_ids[0])
        subprocess.check_output(shell_command, shell=True)
        res = requests.get(self.__class__.nodes_address[1] + '/keyValue-store/' + self.__class__.key1)
        d = res.json()
        self.assertEqual(res.status_code, 501)

# dummy test to indicate end of tests
    def test_z_taking_down_primary_instance(self):
        print("| Finished.")
        self.__class__.all_tests_done = True

    def tearDown(self):
        if self.__class__.all_tests_done:
            print("\nKilling all alive nodes.")
            shell_command = "docker stop $(docker ps -q)"
            subprocess.check_output(shell_command, shell=True)


if __name__ == '__main__':
    unittest.main()
