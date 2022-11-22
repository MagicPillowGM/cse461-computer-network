# Part 4 of UWCSE's Project 2
#
# based on Lab Final from UCSC's Networking Class
# which is based on of_tutorial by James McCauley

from pox.core import core
import pox.openflow.libopenflow_01 as of
from pox.lib.addresses import IPAddr, IPAddr6, EthAddr
from pox.lib.packet.ethernet import ethernet
from pox.lib.packet.arp import arp

log = core.getLogger()

#statically allocate a routing table for hosts
#MACs used in only in part 4
IPS = {
  "h10" : ("10.0.1.10", '00:00:00:00:00:01'),
  "h20" : ("10.0.2.20", '00:00:00:00:00:02'),
  "h30" : ("10.0.3.30", '00:00:00:00:00:03'),
  "serv1" : ("10.0.4.10", '00:00:00:00:00:04'),
  "hnotrust" : ("172.16.10.100", '00:00:00:00:00:05'),
}

class Part4Controller (object):
  """
  A Connection object for that switch is passed to the __init__ function.
  """
  def __init__ (self, connection):
    print (connection.dpid)
    # Keep track of the connection to the switch so that we can
    # send it messages!
    self.connection = connection

    # This binds our PacketIn event listener
    connection.addListeners(self)
    #use the dpid to figure out what switch is being created
    if (connection.dpid == 1):
      self.s1_setup()
    elif (connection.dpid == 2):
      self.s2_setup()
    elif (connection.dpid == 3):
      self.s3_setup()
    elif (connection.dpid == 21):
      self.cores21_setup()
    elif (connection.dpid == 31):
      self.dcs31_setup()
    else:
      print ("UNKNOWN SWITCH")
      exit(1)

  def s1_setup(self):
    #put switch 1 rules here
    fm = of.ofp_flow_mod()
    fm.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    self.connection.send(fm)

  def s2_setup(self):
    #put switch 2 rules here
    fm = of.ofp_flow_mod()
    fm.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    self.connection.send(fm)

  def s3_setup(self):
    #put switch 3 rules here
    fm = of.ofp_flow_mod()
    fm.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    self.connection.send(fm)

  def cores21_setup(self):
    #put core switch rules here
    # ICMP
    fm = of.ofp_flow_mod()
    fm.match.dl_type = 0x0800
    fm.match.nw_proto = 1
    fm.match.nw_src = IPS["hnotrust"][0]
    self.connection.send(fm)
    # any IP
    fm = of.ofp_flow_mod()
    fm.match.dl_type = 0x0800
    fm.match.nw_src = IPS["hnotrust"][0]
    fm.match.nw_des = IPS["serv1"][0]
    self.connection.send(fm)

    # others acceptable
    # fm = of.ofp_flow_mod()
    # fm.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    # self.connection.send(fm)

  def dcs31_setup(self):
    #put datacenter switch rules here
    fm = of.ofp_flow_mod()
    fm.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    self.connection.send(fm)

  #used in part 4 to handle individual ARP packets
  #not needed for part 3 (USE RULES!)
  #causes the switch to output packet_in on out_port
  def resend_packet(self, packet_in, out_port):
    msg = of.ofp_packet_out()
    msg.data = packet_in
    action = of.ofp_action_output(port = out_port)
    msg.actions.append(action)
    self.connection.send(msg)

  def _handle_PacketIn (self, event):
    """
    Packets not handled by the router rules will be
    forwarded to this method to be handled by the controller
    """

    packet = event.parsed # This is the parsed packet data.
    if not packet.parsed:
      log.warning("Ignoring incomplete packet")
      return

    packet_in = event.ofp # The actual ofp_packet_in message.
    port_num = event.port

    print ("Unhandled packet from " + str(self.connection.dpid) + ":" + packet.dump())

    if (packet.type == packet.ARP_TYPE and packet.payload.opcode == arp.REQUEST):
          fm = of.ofp_flow_mod()
          fm.match.dl_type = 0x0800
          # fm.priority = 7
          fm.match.nw_dst = packet.payload.protosrc
          # fm.actions.append(of.ofp_action_dl_addr.set_src())
          fm.actions.append(of.ofp_action_dl_addr.set_dst(packet.src))
          fm.actions.append(of.ofp_action_output(port=port_num))
          self.connection.send(fm)
               
          arp_reply_msg = arp()
          arp_reply_msg.hwsrc = EthAddr("00:00:00:00:00:10")
          arp_reply_msg.hwdst = packet.src
          arp_reply_msg.opcode = arp.REPLY
          arp_reply_msg.protosrc = packet.payload.protodst
          arp_reply_msg.protodst = packet.payload.protosrc
          
          # wrap the ARP Reply msg into Ethernet
          ether = ethernet()
          ether.type = ethernet.APR_TYPE
          ether.src = EthAddr("00:00:00:00:00:11")
          ether.dst = packet.src
          ether.payload = arp_reply_msg
          
          # send packet
          self.resend_packet(ether, port_num)

def launch ():
  """
  Starts the component
  """
  def start_switch (event):
    log.debug("Controlling %s" % (event.connection,))
    Part3Controller(event.connection)
  core.openflow.addListenerByName("ConnectionUp", start_switch)
