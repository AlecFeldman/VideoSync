import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class VideoSync
{
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		int option;
		int clientPort = 4000;
		
		Scanner keys = new Scanner(System.in);
		
		ArrayList<PeerAddress> clientAddresses = new ArrayList<>();
		
		Number160 theater = new Number160();
		Number160 clientID = new Number160(new Random());
		
		Peer client = new PeerBuilder(clientID).ports(clientPort).start();
		PeerDHT clientData = new PeerBuilderDHT(client).start();
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		FutureGet getData;
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		keys.nextLine();
		
		if (option == 1)
		{
			masterBuilder.peerAddress(client.peerAddress());
			
			System.out.print("Create theater: ");
			theater = Number160.createHash(keys.nextLine());
		}
		else if (option == 2)
		{
			System.out.print("Enter IP address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keys.nextLine()));
			
			System.out.print("Enter port: ");
			masterBuilder.ports(keys.nextInt()).start();
			
			keys.nextLine();
			
			System.out.print("Enter theater: ");
			theater = Number160.createHash(keys.nextLine());
		}
		
		master = masterBuilder.start();
		
		master.addListener(new BaseFutureAdapter<FutureBootstrap>()
		{
			@Override
			public void operationComplete(FutureBootstrap master)
			{
				if(master.isSuccess())
				{
					System.out.println("Successfully connected to " +
									   master.bootstrapTo().iterator().next().inetAddress() +
									   " on port " +
									   master.bootstrapTo().iterator().next().tcpPort() +
									   ".");
				}
				else
				{
					System.out.println("Failed to connect to " +
							   		   master.bootstrapTo().iterator().next().inetAddress() +
							   		   " on port " +
							   		   master.bootstrapTo().iterator().next().tcpPort() +
							   		   ".");
				}
			}
		});
		
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request)
			{
				System.out.println("Sender: " + sender + " Request: " + request);
				return "success";
			}
		});
		
		if (option == 1)
		{
			getData = clientData.get(theater).start();
			getData.awaitUninterruptibly();
			System.out.println("Creating theater...");
			clientData.put(theater).data(new Data(new ArrayList<PeerAddress>())).start().awaitUninterruptibly();
		}
		
		getData = clientData.get(theater).start();
		getData.awaitUninterruptibly();
		clientAddresses = (ArrayList<PeerAddress>) getData.dataMap().values().iterator().next().object();
		clientAddresses.add(client.peerAddress());
		clientData.put(theater).data(new Data(clientAddresses)).start().awaitUninterruptibly();
		
		for (PeerAddress p : clientAddresses)
		{
			FutureDirect futureDirect = client.sendDirect(p).object("Hello from desktop!").start();
			futureDirect.awaitUninterruptibly();
		}
	}
}