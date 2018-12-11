import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;

public class Theater
{
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException
	{
		int option;
		
		Scanner keyboard = new Scanner(System.in);
		
		ServerSocket randomSocket = new ServerSocket(0);
		
		randomSocket.close();
		
		Number160 clientID = new Number160(new Random());
		Number160 videoKey = Number160.createHash("video");
		Number160 audioKey = Number160.createHash("audio");
		
		Peer client = new PeerBuilder(clientID).ports(randomSocket.getLocalPort()).start();
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keyboard.nextInt();
		keyboard.nextLine();
		
		if (option == 1)
		{
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			// Temporary
			System.out.println("IP: " + client.peerAddress().inetAddress().getHostAddress() +
							   "\nPort: " + client.peerAddress().tcpPort());
			keyboard.nextLine();
			// Temporary
			
			new MediaMaster("./resources/test.mp4", client, videoKey, audioKey);
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keyboard.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keyboard.nextInt());
			keyboard.nextLine();
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			new MediaNetwork(client);
		}
		
		keyboard.close();
	}
}