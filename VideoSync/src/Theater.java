import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class Theater
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException
	{
		int option;
		
		Scanner keyboard = new Scanner(System.in);
		
		ServerSocket randomSocket = new ServerSocket(0);
		
		randomSocket.close();
		
		Number160 theaterKey = new Number160();
		Number160 clientID = new Number160(new Random());
		
		Peer client = new PeerBuilder(clientID).ports(randomSocket.getLocalPort()).start();
		PeerDHT clientData = new PeerBuilderDHT(client).start();
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		FutureGet getData;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keyboard.nextInt();
		keyboard.nextLine();
		
		if (option == 1)
		{
			System.out.print("Create theater: ");
			theaterKey = Number160.createHash(keyboard.nextLine());
			
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			// Temporary
			MediaNetwork.createPacketListener(client);
			System.out.println("IP: " + client.peerAddress().inetAddress().getHostAddress() +
							   "\nPort: " + client.peerAddress().tcpPort());
			keyboard.nextLine();
			// Temporary
			
			MediaMaster.play("./resources/test.mp4", clientData, theaterKey);
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keyboard.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keyboard.nextInt());
			keyboard.nextLine();
			
			System.out.print("Enter theater: ");
			theaterKey = Number160.createHash(keyboard.nextLine());
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			MediaNetwork.createPacketListener(client);
		}
	}
}