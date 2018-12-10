import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import net.tomp2p.dht.FutureSend;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.Number160;

public class MediaMaster
{
	public static void play(String mediaFile, PeerDHT clientData, Number160 theaterKey) throws InterruptedException, IOException
	{
		int totalStreams;
		int packetIndex;
		int videoStreamIndex = -1;
		int audioStreamIndex = -1;
		
		AtomicBoolean isMasterFinished = new AtomicBoolean(false);
		
		Demuxer mediaContainer = Demuxer.make();
		DemuxerStream stream = null;
		
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
		MediaPacket packet = MediaPacket.make();
		
		RunnableVideo video;
		RunnableAudio audio;
		
		Thread videoThread;
		Thread audioThread;
		
		mediaContainer.open(mediaFile, null, false, true, null, null);
		totalStreams = mediaContainer.getNumStreams();
		
		for (int i = 0; i < totalStreams; i++)
		{
			stream = mediaContainer.getStream(i);
			mediaDecoder = stream.getDecoder();
			
			if (mediaDecoder != null)
			{
				if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
				{
					videoDecoder = mediaDecoder;
					videoStreamIndex = i;
				}
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioStreamIndex = i;
				}
			}
	    }
		
		video = new RunnableVideo(videoDecoder, isMasterFinished);
		audio = new RunnableAudio(audioDecoder, isMasterFinished);
		
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		videoThread.start();
		audioThread.start();
		
		while (mediaContainer.read(packet) >= 0)
		{
			packetIndex = packet.getStreamIndex();
			
			if (packetIndex == videoStreamIndex)
			{
				video.addVideoPacket(packet);
			}
			else if (packetIndex == audioStreamIndex)
			{
				audio.addAudioPacket(packet);
			}
			
			clientData.send(theaterKey).object(new MediaPacketSerialized(packet)).start();
			//futureSend.awaitUninterruptibly();
			//for (Object object : futureSend.rawDirectData2().values()) {
			//	System.err.println("got:" + object);
			//}
		}
		
		isMasterFinished.set(true);
		
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
}
