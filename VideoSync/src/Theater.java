import java.io.IOException;

import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import io.humble.video.Decoder;

public class Theater
{
	public static void main(String[] args) throws InterruptedException, IOException
	{
		String mediaFile = "./resources/test.mp4";
		
		Demuxer mediaContainer = Demuxer.make();
		
		mediaContainer.open(mediaFile, null, false, true, null, null);
		
		int packetID;
		int videoStreamID = -1;
		int audioStreamID = -1;
		int totalStreams = mediaContainer.getNumStreams();
		
		DemuxerStream stream = null;
		
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
		MediaPacket packet = MediaPacket.make();
		
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
		
		VideoRunnable video = new VideoRunnable(videoDecoder);
		Thread videoThread = new Thread(video);
		videoThread.start();
		
		AudioRunnable audio = new AudioRunnable(audioDecoder);
		Thread audioThread = new Thread(audio);
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
		}
		
		mediaContainer.close();
		audio.stopAudio();
		video.stopVideo();
	}
}