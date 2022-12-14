## Group Members
George Ma (zma9977)<br>
Linden Gan (lg57)<br>
Kenny Mai (mhq520)<br>

## Instructions
For experiment in Part 2, run  
`sudo ./run.sh`  
For experiment in Part 3, run  
`sudo ./run_bbr.sh`

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
c) Minimize the number of network bottlenecks, or minimize the data rate differences of two adjacent links.  
d) Design algorithm to better predict congestion so that sender can lower the data rate before congestion happens.  

## Part 3 questions
### 1. What is the average webpage fetch time and its standard deviation when q=20 and q=100?
mean for queue size 20: 6.31263861111111  
stdev for queue size 20: 6.773536224410574  
mean for queue size 100: 5.814845157894736  
stdev for queue size 100: 3.120990525984103 

### 2. Compare the webpage fetch time between q=20 and q=100 from Part 3. Which queue length gives a lower fetch time? How is this different from Part 2?
Between q=20 and q=100, q=100 actually gives shorter time, unlike Part 2 in which q=20 gives shorter time.
It indicates that bbr algorithm can do better in solving bufferbloat problem in longer queue, but still has problem when queue is short.

### 3. Do you see the difference in the queue size graphs from Part 2 and Part 3? Give a brief explanation for the result you see.
For q=20, the queue size graphs look similar because both queues are overflowed, though the mean number of packets for Part 2 is higher than Part 3's,
since bbr algorithm in Part 3 tries to mitigate this.  
For q=100, however, Part 2's graph reaches 100 in y axis freqently,
but Part 3 doesn't. And, Part 2's average packet numbers is much higher than Part 3's. This is because Part 3's bbr algorithm avoids buffer's overflow
and controls the number of packets in a relatively small number.

### 4. Do you think we have solved bufferbloat problem? Explain your reasoning.
Not completely. bbr algorithm can indeed do better for longer queue, but for shorter queue, packets can still overflow the queue and get lost.
So, we haven't solve bufferbloat problem. However, shorter queue react more quickly, so even if it's overflowed, it won't cause severe congestion than longer queue does.
