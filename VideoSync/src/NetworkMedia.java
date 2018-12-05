import io.humble.video.MediaPacket;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

public class NetworkMedia
{
	private static int packetID;
	private int videoStreamID;
	private int audioStreamID;
	
	private static MediaPacket packet;
	
	private VideoRunnable video;
	private AudioRunnable audio;
	
	private Thread videoThread = new Thread(video);
	private Thread audioThread = new Thread(video);
	
	public NetworkMedia(int videoStreamID, int audioStreamID)
	{
		this.videoStreamID = videoStreamID;
		this.audioStreamID = audioStreamID;
	}
	
	public static void createPacketListener(Peer client)
	{
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request) throws Exception
			{
				packet = (MediaPacket) request;
				packetID = packet.getStreamIndex();
				System.out.println(packetID);
				
				//if (packetID == videoStreamID)
				//{
				//	video.addVideoPacket(packet);
				//}
				//else if (packetID == audioStreamID)
				//{
				//	audio.addAudioPacket(packet);
				//}
				return "success";
			}
		});
	}
}
