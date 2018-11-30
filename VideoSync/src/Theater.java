import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaAudio;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.awt.ImageFrame;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import io.humble.video.javaxsound.AudioFrame;
import io.humble.video.javaxsound.MediaAudioConverter;
import io.humble.video.javaxsound.MediaAudioConverterFactory;
import io.humble.video.Decoder;

public class Theater
{
	public static void main(String[] args) throws InterruptedException, IOException
	{
		String filename = "./resources/test.mp4";
		
		Demuxer mediaContainer = Demuxer.make();
		
		mediaContainer.open(filename, null, false, true, null, null);
		
		int totalStreams = mediaContainer.getNumStreams();
		int videoStreamID = -1;
		int audioStreamID = -1;
		
		DemuxerStream stream = null;
		
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
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
		
		videoDecoder.open(null, null);
		audioDecoder.open(null, null);
		
		MediaPacket packet = MediaPacket.make();
		
		Thread video = new Thread(new VideoThread(mediaContainer, videoDecoder, videoStreamID, packet));
		Thread audio = new Thread(new AudioThread(mediaContainer, audioDecoder, audioStreamID, packet));
		
		video.start();
		//audio.start();
		
		//mediaContainer.close();
	}
}