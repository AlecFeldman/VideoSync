import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Scanner;

public class VideoSync
{
	public static void main(String[] args) throws IOException
	{
		int option;
		
		String ip = "";
		String topicName = "";
		
		Scanner keys = new Scanner(System.in);
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		
		keys.nextLine();
		
		if (option == 1)
		{
			ip = getIPv4Address();
			
			System.out.print("Create a topic: ");
			topicName = keys.nextLine();
		}
		else if (option == 2)
		{
			System.out.print("Enter IP address: ");
			ip = keys.nextLine();
			
			System.out.print("Enter a topic: ");
			topicName = keys.nextLine();
		}
		
		joinServer(option, ip, topicName, keys);
		keys.close();
		System.exit(0);
	}
	
	public static void joinServer(int option, String ip, String topicName, Scanner keys) throws IOException
	{
		boolean quitServer = false;
		
		String message = "";
		
		try
		{
			// ID must be different for each client.
			PublishSubscribeImpl peer = new PublishSubscribeImpl(0, ip, new MessageListenerImpl(0));
			
			if (option == 1)
				peer.createTopic(topicName);
			
			peer.subscribetoTopic(topicName);
			
			while (!quitServer)
			{
				System.out.print("> ");
				message = keys.nextLine();
				
				if (message.startsWith("/"))
				{
					// More server commands will be added here later.
					switch(message.substring(1))
					{
						case "quit":
							quitServer = true;
							break;
						default:
							System.out.println("Different commands will be printed here.");
					}
				}
				else
				{
					peer.publishToTopic(topicName, message);
				}
			}
			
			System.out.println("Quiting server...");
			peer.unsubscribeFromTopic(topicName);
			peer.leaveNetwork();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getIPv4Address()
	{
		String ip = "";
		
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			
			while (interfaces.hasMoreElements())
			{
				NetworkInterface iface = interfaces.nextElement();
				
				if (iface.isLoopback() || !iface.isUp())
					continue;
				
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				
				while(addresses.hasMoreElements())
				{
					InetAddress addr = addresses.nextElement();
					
					if (addr instanceof Inet6Address)
						continue;
					
					ip = addr.getHostAddress();
				}
			}
		}
		catch (SocketException e)
		{
			throw new RuntimeException(e);
		}
		
		return ip;
	}
}