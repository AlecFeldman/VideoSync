import java.io.IOException;
import java.nio.ByteBuffer;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.MediaAudio;
import io.humble.video.MediaPacket;
import io.humble.video.javaxsound.AudioFrame;
import io.humble.video.javaxsound.MediaAudioConverter;
import io.humble.video.javaxsound.MediaAudioConverterFactory;

public class AudioThread implements Runnable
{
	private int audioStreamID;
	
	private Demuxer mediaContainer;
	
	private Decoder audioDecoder;
	
	private MediaPacket packet;
	
	public AudioThread(Demuxer mediaContainer, Decoder audioDecoder, int audioStreamID, MediaPacket packet)
	{
		this.mediaContainer = mediaContainer;
		this.audioDecoder = audioDecoder;
		this.audioStreamID = audioStreamID;
		this.packet = packet;
	}
	
	public void run()
	{
		int offset;
		int bytesRead;
		int packetID;
		
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
		
		try
		{
			while (mediaContainer.read(packet) >= 0)
			{
				offset = 0;
				bytesRead = 0;
				packetID = packet.getStreamIndex();
				
				if (packetID == audioStreamID)
				{
					do
					{
						bytesRead += audioDecoder.decode(samples, packet, offset);
						if (samples.isComplete())
						{
							rawAudio = audioConverter.toJavaAudio(rawAudio, samples);
							audioConnection.play(rawAudio);
						}
						offset += bytesRead;
					} while (offset < packet.getSize());
				}
			}
		}
		catch (InterruptedException | IOException e)
		{
			e.printStackTrace();
		}
		
		audioConnection.dispose();
	}
}
