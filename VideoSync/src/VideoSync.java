import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class VideoSync
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException
	{
		int option;
		
		/*
		 * _topic_name represents the "key" that holds a pair of PeerAddress objects. _obj can be anything,
		 * but for now it is declared as a string. If listening on another address, a for loop will iterate
		 * over each PeerAddress and send _obj to be displayed in console. This is effectively a broadcast.
		 * A peer is first created with its address set to the address of the computer, and is then bootstrapped
		 * to _master address. This could either be its own address, or a remote address. For basic communication
		 * between peers, each computer must know its own address, as well as every address of all other peers.
		 * Once connection is established, other pieces of data become more important. The master peer should always
		 * have control over which videos are playing, as well as at what time they are currently at. They should also
		 * be able to give access to other peers at their leisure. If they wish to see a peer removed, there should be
		 * a command to do so. All actions the master peer wishes to perform can be done either through commands in a
		 * console, or a series of menus. This means for modularity, this program should be able to function without a
		 * front end.
		 * 
		 * Current issues: The code below is even more basic than last week's example, but being able to break code
		 * into its basic parts will allow for reconstruction to fit the program better. Due to DHCP, ipv4 addresses
		 * change whenever a client reconnects to the network. This could make testing and establishing connection
		 * difficult. What centralized video sync services do is use a room invitation key. Perhaps a key could be
		 * generated containing both the IP and port. This way, users won't have to figure out their current ipv4
		 * address in order to invite more peers. The biggest hurdle right now is cleaning up the code below without
		 * breaking functionality.
		 */
		String _topic_name = "Videos";
		String _obj = "Hello from desktop!";
		String _master = "10.0.0.243";
		
		Scanner keys = new Scanner(System.in);
		
		HashSet<PeerAddress> peers_on_topic = new HashSet<>();
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		
		PeerDHT peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(0)).ports(4000).start()).start();
		
		FutureBootstrap fb = peer.peer().bootstrap().inetAddress(InetAddress.getByName(_master)).ports(4000).start();
		fb.awaitUninterruptibly();
		if(fb.isSuccess())
		{
			peer.peer().discover().peerAddress(peer.peerAddress()).start().awaitUninterruptibly();
		}
		
		peer.peer().objectDataReply(new ObjectDataReply()
		{
			public Object reply(PeerAddress sender, Object request) throws Exception
			{
				System.out.println(request);
				return "success";
			}
		});
		
		FutureGet futureGet;
		
		if (option == 1)
		{
			System.out.println("Creating topic...");
			futureGet = peer.get(Number160.createHash(_topic_name)).start();
			futureGet.awaitUninterruptibly();
			if (futureGet.isSuccess() && futureGet.isEmpty())
			{
				peer.put(Number160.createHash(_topic_name)).data(new Data(new HashSet<PeerAddress>())).start().awaitUninterruptibly();
			}
		}
		
		futureGet = peer.get(Number160.createHash(_topic_name)).start();
		futureGet.awaitUninterruptibly();
		if (futureGet.isSuccess())
		{
			try
			{
				peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
				peers_on_topic.add(peer.peer().peerAddress());
				peer.put(Number160.createHash(_topic_name)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		futureGet = peer.get(Number160.createHash(_topic_name)).start();
		futureGet.awaitUninterruptibly();
		if (futureGet.isSuccess())
		{
			try
			{
				peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			for(PeerAddress p : peers_on_topic)
			{
				FutureDirect futureDirect = peer.peer().sendDirect(p).object(_obj).start();
				futureDirect.awaitUninterruptibly();
			}
		}
	}
}