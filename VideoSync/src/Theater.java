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

	/**
	 * @author Alec Feldman
	 * @author Liam Gibbons
	 * @version 0.2
	 * @since 11/30/2018
	 * 
	 * This class is a test of our preliminary player. It works using the humble video library through Maven. Running the main method
	 * will open a player screen, which currently just plays a test-video file.
	 */
{
	
	public static void main(String[] args) throws InterruptedException, IOException
	{
		// So the player can find the file being used. Currently, it must be in MP4 form.
		String filename = "./resources/test.mp4";
		
		// Creates the demuxer, which splits the audio and the video out of the MP4 file.
		Demuxer mediaContainer = Demuxer.make();
		
		// (String url, DemuxerFormat format, boolean streamsCanBeAddedDynamically, 
		// boolean queryStreamMetaData, KeyValueBag options, KeyValueBag optionsNotSet)
		
		
		/**
		 * @param url The URL from which the container refers to for the file. <br>
		 * @param format Points to an object from which the container refers to for a way to format itself. <br>
		 * Leaving it null will let the container uses it's defaults for what it finds.<br>
		 * @param streamsCanBeAddedDynamically If true, streams can be added at any time, and the container will<br>
		 *  be listening for this to occur.<br>
		 * @param queryStreamMetaData Just leave this as true. There's no reason to change it, and false breaks it.<br>
		 * @param options If this isn't nulled, key-value pairs will be used on data going through.<br>
		 * @param optionsNotSet If not nulled, the KeyValueBag will be cleared and it'll make sure that<br>
		 * key value assignments are properly set.<br>
		 */
		mediaContainer.open(filename, null, false, true, null, null);
		
		// How many streams are on this container.
		int totalStreams = mediaContainer.getNumStreams();
		
		// Setting the stream ID's to the beginning. This is a method to try to keep them sync'd on the client side.
		int videoStreamID = -1;
		int audioStreamID = -1;
		
		// Declaring the stream.
		DemuxerStream stream = null;
		
		// Declaring the decoders, which will be set later.
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
		// Running the stream.
		for (int i = 0; i < totalStreams; i++)
		{
			// Getting the stream data
			stream = mediaContainer.getStream(i);
			
			// Selecting the decoder
			mediaDecoder = stream.getDecoder();
			
			// When there is no decoder selected:
			if (mediaDecoder != null)
			{
				// Sends packet as a video packet
				if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
				{
					videoDecoder = mediaDecoder;
					videoStreamID = i;
				}
				
				// Sends packet as an audio packet
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioStreamID = i;
				}
			}
	    }
		
		// Opens the decoder, with nulled key-value pair settings.
		videoDecoder.open(null, null);
		audioDecoder.open(null, null);
		
		// Creates the data packet to be transferred over the network, or to be read by the player on the client-side.
		MediaPacket packet = MediaPacket.make();
		
		// Declares video and audio
		Thread video = new Thread(new VideoThread(mediaContainer, videoDecoder, videoStreamID, packet));
		Thread audio = new Thread(new AudioThread(mediaContainer, audioDecoder, audioStreamID, packet));
		
		video.start();
		//audio.start();
		
		//mediaContainer.close();
	}
}