import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import io.humble.video.Decoder;

/**
 * 
 * @author Alec Feldman
 * @author Liam Gibbons
 * @version 0.2.1
 * @since 12/3/2018
 * 
 * This is the main class for the app's video player. It accepts video streams, and plays the video and audio using humble 
 * video. It also handles encoding packets to be sent out over tomp2p. 
 *
 */

public class Theater
{
	public static void main(String[] args) throws InterruptedException, IOException
	{
		
		// Declaring total amount of streams (Will be set further down)
		int totalStreams;
		// Declaring the packet ID data variable (Will be used further in)
		int packetID;
		// Both stream ID's are set to -1 until they are set by the decoders.
		int videoStreamID = -1;
		int audioStreamID = -1;
		
		// Test file for now, will be set for streaming soon.
		String mediaFile = "./resources/test.mp4";
		
		// Used in a method later on for keeping the stream going.
		AtomicBoolean isMainFinished = new AtomicBoolean(false);
		
		// Declaring the main container for the stream, which is used to separate the audio and video parts of the video file
		Demuxer mediaContainer = Demuxer.make();
		
		// Setting the stream to null for now, until the decoders are set and the stream has anything to run.
		DemuxerStream stream = null;
		
		// The decoders for the media, audio, and video. They're set in a loop further down.
		Decoder mediaDecoder = null;
		Decoder videoDecoder = null;
		Decoder audioDecoder = null;
		
		// Declaring a media packet, and creating one.
		MediaPacket packet = MediaPacket.make();
		
		// Declaring the audio and video parts of the stream.
		VideoRunnable video;
		AudioRunnable audio;
		
		// Setting up dual threads, to be run concurrently soon.
		Thread videoThread;
		Thread audioThread;
		
		// Opens a media container to run the media file, a default demux format, disabled dynamic streams
		// Meta data queries enabled, and without key value pairs.
		mediaContainer.open(mediaFile, null, false, true, null, null);
		totalStreams = mediaContainer.getNumStreams();
		
		// For loop to set up the video and audio decoders.
		for (int i = 0; i < totalStreams; i++)
		{
			// Since they're run in multiple threads, it sets two different decoders, to be run concurrently.
			stream = mediaContainer.getStream(i);
			mediaDecoder = stream.getDecoder();
			
			if (mediaDecoder != null)
			{
				// Setting the video coder
				if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
				{
					videoDecoder = mediaDecoder;
					videoStreamID = i;
				}
				// Setting the audio coder
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioStreamID = i;
				}
			}
	    }
		
		// Decodes the data to be sent to the stream
		video = new VideoRunnable(videoDecoder, isMainFinished);
		audio = new AudioRunnable(audioDecoder, isMainFinished);
		
		// Sends the data to each thread.
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		// Then starts the stream.
		videoThread.start();
		audioThread.start();
		
		// Runs until packets are finished being sent. Will close automatically upon disconnection.
		while (mediaContainer.read(packet) >= 0)
		{
			// Takes each packet based on ID.
			packetID = packet.getStreamIndex();
			
			// Add's the packet into the stream based on audio and video.
			if (packetID == videoStreamID)
			{
				video.addVideoPacket(packet);
			}
			else if (packetID == audioStreamID)
			{
				audio.addAudioPacket(packet);
			}
		}
		
		// Finishes the player method once the while loop is finished running.
		isMainFinished.set(true);
		
		// Ends the threads, then closes the player.
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
}