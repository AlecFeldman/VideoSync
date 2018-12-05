import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;

import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.PeerAddress;

public class MasterMedia
{
	public static void play(String mediaFile, Peer client, ArrayList<PeerAddress> connectedClients) throws InterruptedException, IOException
	{
		int totalStreams;
		int packetID;
		int videoStreamID = -1;
		int audioStreamID = -1;
		
		AtomicBoolean isMasterFinished = new AtomicBoolean(false);
		
		Demuxer mediaContainer = Demuxer.make();
		DemuxerStream stream = null;
		
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
		MediaPacket packet = MediaPacket.make();
		
		VideoRunnable video;
		AudioRunnable audio;
		
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
					videoStreamID = i;
				}
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioStreamID = i;
				}
			}
	    }
		
		video = new VideoRunnable(videoDecoder, isMasterFinished);
		audio = new AudioRunnable(audioDecoder, isMasterFinished);
		
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		videoThread.start();
		audioThread.start();
		
		while (mediaContainer.read(packet) >= 0)
		{
			packetID = packet.getStreamIndex();
			
			if (packetID == videoStreamID)
			{
				video.addVideoPacket(packet);
			}
			else if (packetID == audioStreamID)
			{
				audio.addAudioPacket(packet);
			}
			
			for(PeerAddress c : connectedClients)
			{
				client.sendDirect(c).object(packet).start();
			}
		}
		
		isMasterFinished.set(true);
		
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
}
