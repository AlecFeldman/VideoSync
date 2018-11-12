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
		
		String _topic_name = "Videos";
		String _obj = "Hello from laptop!";
		
		Scanner keys = new Scanner(System.in);
		
		HashSet<PeerAddress> peers_on_topic = new HashSet<>();
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		
		PeerDHT peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(0)).ports(4000).start()).start();
		
		FutureBootstrap fb = peer.peer().bootstrap().inetAddress(InetAddress.getByName("127.0.0.1")).ports(4000).start();
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