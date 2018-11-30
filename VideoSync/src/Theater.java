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
		
		MediaPicture videoFrame = MediaPicture.make(
				videoDecoder.getWidth(),
				videoDecoder.getHeight(),
				videoDecoder.getPixelFormat());
		
		MediaPictureConverter videoConverter =
				MediaPictureConverterFactory.createConverter(
						MediaPictureConverterFactory.HUMBLE_BGR_24,
						videoFrame);
		
		ImageFrame window = ImageFrame.make();
		BufferedImage image = null;
		
		MediaAudio samples = MediaAudio.make(
				audioDecoder.getFrameSize(),
				audioDecoder.getSampleRate(),
				audioDecoder.getChannels(),
				audioDecoder.getChannelLayout(),
				audioDecoder.getSampleFormat());
		
		MediaAudioConverter audioConverter =
				MediaAudioConverterFactory.createConverter(
					MediaAudioConverterFactory.DEFAULT_JAVA_AUDIO,
					samples);
		
		AudioFrame audioConnection = AudioFrame.make(audioConverter.getJavaFormat());
		ByteBuffer rawAudio = null;
		
		int offset;
		int bytesRead;
		int packetID;
		
		MediaPacket packet = MediaPacket.make();
		
		while (mediaContainer.read(packet) >= 0)
		{
			offset = 0;
			bytesRead = 0;
			packetID = packet.getStreamIndex();
			
			if (packetID == videoStreamID)
			{	
				do
				{
					bytesRead += videoDecoder.decode(videoFrame, packet, offset);
					if (videoFrame.isComplete())
					{
						image = videoConverter.toImage(image, videoFrame);
						window.setImage(image);
					}
					offset += bytesRead;
				} while (offset < packet.getSize());
			}
			else if (packetID == audioStreamID)
			{
				do
				{
					bytesRead += audioDecoder.decode(samples, packet, offset);
					if (samples.isComplete())
					{
						rawAudio = audioConverter.toJavaAudio(rawAudio, samples);
						//audioConnection.play(rawAudio);
					}
					offset += bytesRead;
				} while (offset < packet.getSize());
			}
		}
		
		mediaContainer.close();
		audioConnection.dispose();
		window.dispose();
	}
}