import java.awt.image.BufferedImage;
import java.io.IOException;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.awt.ImageFrame;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

public class VideoThread implements Runnable
{
	private int videoStreamID;
	
	private Demuxer mediaContainer;
	
	private Decoder videoDecoder;
	
	private MediaPacket packet;
	
	public VideoThread(Demuxer mediaContainer, Decoder videoDecoder, int videoStreamID, MediaPacket packet)
	{
		this.mediaContainer = mediaContainer;
		this.videoDecoder = videoDecoder;
		this.videoStreamID = videoStreamID;
		this.packet = packet;
	}
	
	public void run() 
	{
		int offset;
		int bytesRead;
		int packetID;
		
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
		
		try
		{
			while (mediaContainer.read(packet) >= 0)
			{
				Thread.sleep(15);
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
			}
		}
		catch (InterruptedException | IOException e)
		{
			e.printStackTrace();
		}
		
		window.dispose();
	}
}