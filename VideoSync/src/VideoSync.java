import java.io.IOException;

public class VideoSync
{
	public static void main(String[] args) throws InterruptedException
	{
		class MessageListenerImpl implements MessageListener{
			int peerid;
			public MessageListenerImpl(int peerid)
			{
				this.peerid=peerid;
			}
			public Object parseMessage(Object obj) {
				System.out.println(peerid+"] (Direct Message Received) "+obj);
				return "success";
			}
			
		}
		
		try {
			PublishSubscribeImpl peer0 = new PublishSubscribeImpl(0, "127.0.0.1", new MessageListenerImpl(0));
			
			peer0.createTopic("Video");
			peer0.subscribetoTopic("Video");
			peer0.publishToTopic("Video", "Hello from peer0!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}