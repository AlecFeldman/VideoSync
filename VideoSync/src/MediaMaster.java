import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;

public class MediaMaster
{
	public MediaMaster(String mediaFile, Peer client, Number160 videoKey, Number160 audioKey) throws InterruptedException, IOException
	{
		int totalStreams;
		int videoIndex = -1;
		int audioIndex = -1;
		
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
					videoIndex = i;
				}
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioIndex = i;
				}
			}
	    }
		
		video = new RunnableVideo(videoDecoder, videoKey, client, isMasterFinished);
		audio = new RunnableAudio(audioDecoder, audioKey, client, isMasterFinished);
		
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		videoThread.start();
		audioThread.start();
		
		while (mediaContainer.read(packet) >= 0)
		{
			if (packet.getStreamIndex() == videoIndex)
			{
				video.addPacket(packet);
			}
			else if (packet.getStreamIndex() == audioIndex)
			{
				audio.addPacket(packet);
			}
		}
		
		isMasterFinished.set(true);
		
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
}
