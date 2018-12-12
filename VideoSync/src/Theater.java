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
		Number160 mediaKey = Number160.createHash("media");
		
		Peer client = new PeerBuilder(clientID).ports(randomSocket.getLocalPort()).start();
		
		FutureBootstrap master;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keyboard.nextInt();
		keyboard.nextLine();
		
		if (option == 1)
		{
			Media mediaMaster = new Media("./resources/test.mp4", client, mediaKey);
			
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			// Temporary
			System.out.println("IP: " + client.peerAddress().inetAddress().getHostAddress() +
							   "\nPort: " + client.peerAddress().tcpPort());
			keyboard.nextLine();
			// Temporary
			
			mediaMaster.playMedia();
		}
		else if (option == 2)
		{
			BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
			
			Media mediaClient = new Media(client, mediaKey);
			
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keyboard.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keyboard.nextInt());
			keyboard.nextLine();
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			mediaClient.waitForMedia();
		}
		
		keyboard.close();
	}
}