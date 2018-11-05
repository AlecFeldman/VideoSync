import com.corundumstudio.socketio.*;

public class VideoSync
{
	public static void main(String[] args)
	{
		Configuration config = new Configuration();
		config.setHostname("localhost");
		config.setPort(1337);
		config.setPingTimeout(30);
		
		SocketConfig socketConfig = new SocketConfig();
		socketConfig.setReuseAddress(true);
		
		config.setSocketConfig(socketConfig);
		
		SocketIOServer server = new SocketIOServer(config);
	}
}