import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class Theater
{
	public static void main(String[] args) throws IOException
	{
		int masterPort = 4000;
		
		String masterAddress = "10.0.0.244";
		String theater = "Videos";
		String message = "Hello there!";
		
		Number160 theaterKey = Number160.createHash(theater);
		Number160 clientID = new Number160(new Random());
		
		ArrayList<PeerAddress> connectedClients = new ArrayList<>();
		
		Peer client = new PeerBuilder(clientID).ports(4000).start();
		
		PeerDHT clientData = new PeerBuilderDHT(client).start();
		
		FutureBootstrap master = client.bootstrap().inetAddress(InetAddress.getByName(masterAddress)).ports(masterPort).start();
		master.awaitUninterruptibly();
		
		if(master.isSuccess())
		{
			client.discover().peerAddress(client.peerAddress()).start().awaitUninterruptibly();
		}
		
		client.objectDataReply(new ObjectDataReply()
		{
			public Object reply(PeerAddress sender, Object request) throws Exception
			{
				System.out.println("Sender: " + sender + " Request: " + request);
				return "success";
			}
		});
		
		FutureGet getData = clientData.get(theaterKey).start();
		getData.awaitUninterruptibly();
		
		if (getData.isSuccess() && getData.isEmpty())
		{
			System.out.println("Creating theater...");
			clientData.put(theaterKey).data(new Data(new ArrayList<PeerAddress>())).start().awaitUninterruptibly();
		}
		
		getData = clientData.get(theaterKey).start();
		getData.awaitUninterruptibly();
		
		if (getData.isSuccess())
		{
			try
			{
				connectedClients = (ArrayList<PeerAddress>) getData.dataMap().values().iterator().next().object();
				connectedClients.add(client.peerAddress());
				clientData.put(theaterKey).data(new Data(connectedClients)).start().awaitUninterruptibly();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		getData = clientData.get(theaterKey).start();
		getData.awaitUninterruptibly();
		
		if (getData.isSuccess())
		{
			try
			{
				connectedClients = (ArrayList<PeerAddress>) getData.dataMap().values().iterator().next().object();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			for(PeerAddress c : connectedClients)
			{
				FutureDirect futureDirect = client.sendDirect(c).object(message).start();
				futureDirect.awaitUninterruptibly();
			}
		}
	}
}