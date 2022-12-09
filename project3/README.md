## Part 2 questions

### 1. What is the average webpage fetch time and its standard deviation when q=20 and q=100?     
mean for queue size 20: 9.731119714285715  
stdev for queue size 20: 8.31295261111519  
mean for queue size 100: 30.26797566666667  
stdev for queue size 100: 13.200235255190087  

### 2. Why do you see a difference in webpage fetch times with short and large router buffers?
In this experiment, we are making a congestion where input is persistently larger than output, 
since one link has much lower capacity than the other. 
In this case, shorter buffer reacts more quickly to congestion (by discarding packets) than longer buffer, 
since longer buffer takes longer time to fill up. So, in shorter buffer case, sender can detect congestion 
immediately so that it can lower transmission rate to mitigate congestion, so it can be faster. 
On the other hand, sender cannot immediately detect congestion in case of longer buffer, 
so it will cause severe congestion and thus slowing down the transmission speed.

### 3. Bufferbloat can occur in other places such as your network interface card (NIC). Check the output of ifconfig eth0 of your mininet VM. What is the (maximum) transmit queue length on the network interface reported by ifconfig? For this queue size, if you assume the queue drains at 100Mb/s, what is the maximum time a packet might wait in the queue before it leaves the NIC? You can run ifconfig in the mininet shell by inserting CLI(net) in your script and run it. When the shell launches, for example, run h1 ifconfig to run the command on host h1.
    mininet> h1 ifconfig   
    h1-eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500 (maximum transmission unit (MTU) size
    inet 10.0.0.1  netmask 255.0.0.0  broadcast 10.255.255.255
    inet6 fe80::94aa:75ff:fe66:4947  prefixlen 64  scopeid 0x20<link>
    ether 96:aa:75:66:49:47  txqueuelen 1000  (Ethernet)
    RX packets 15  bytes 1222 (1.2 KB)
    RX errors 0  dropped 0  overruns 0  frame 0
    TX packets 8  bytes 656 (656.0 B)
    TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

1000 packets * 1500 bytes/packet * 8 bits/byte / (100 Mb/s * 10^6 bit / 1Mb) = 0.12s

### 4. How does the RTT reported by ping vary with the queue size? Describe the relation between the two.
With buffer size of 20, the RTT ranges from 100ms to 300ms; with buffer size fo 100, the RTT ranges from 500ms to 1300ms.
RTT is positively related to queue size

### 5. Identify and describe two ways to mitigate the bufferbloat problem.
a) Have smaller buffer size. From the experiment we can see that smaller buffer queue size resulted in shorter webpage fetch time. 
Therefore, by decreasing the buffer size we will solve the problem.  
b) Apply slow start to increment window size from 1 to n, instead of suddenly starting with a big window size, 
which causes a burst of packets while other part of the network is not ready yet, which may cause congestion.

## Part 3 questions
### What is the average webpage fetch time and its standard deviation when q=20 and q=100?

### Compare the webpage fetch time between q=20 and q=100 from Part 3. Which queue length gives a lower fetch time? How is this different from Part 2?

### Do you see the difference in the queue size graphs from Part 2 and Part 3? Give a brief explanation for the result you see.

### Do you think we have solved bufferbloat problem? Explain your reasoning.
