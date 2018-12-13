import io.humble.video.Decoder;
import io.humble.video.Codec;
//import io.humble.video.AudioFormat;
//import io.humble.video.PixelFormat;

import java.io.Serializable;

import io.humble.video.AudioChannel.Layout;

public class SerializedStream implements Serializable
{	
	private int videoIndex;
	private int audioIndex;
	private int videoCodecID;
	private int audioCodecID;
	private int videoWidth;
	private int videoHeight;
	private int audioRate;
	//private int audioChannels;
	
	private static final long serialVersionUID = 1L;
	
	//private PixelFormat.Type videoFormat;
	//private AudioFormat.Type audioFormat;
	
	private Layout audioLayout;
	
	public SerializedStream(int videoIndex, int audioIndex, Decoder videoDecoder, Decoder audioDecoder)
	{
		this.videoIndex = videoIndex;
		this.audioIndex = audioIndex;
		videoCodecID = videoDecoder.getCodec().getIDAsInt();
		audioCodecID = audioDecoder.getCodec().getIDAsInt();
		videoWidth = videoDecoder.getWidth();
		videoHeight = videoDecoder.getHeight();
		//videoFormat = videoDecoder.getPixelFormat();
		audioRate = audioDecoder.getSampleRate();
		//audioChannels = audioDecoder.getChannels();
		audioLayout = audioDecoder.getChannelLayout();
		//audioFormat = audioDecoder.getSampleFormat();
	}

	public int getVideoIndex()
	{
		return videoIndex;
	}
	
	public int getAudioIndex()
	{
		return audioIndex;
	}
	
	public Decoder getVideoDecoder()
	{
		Decoder videoDecoder = Decoder.make(Codec.findDecodingCodecByIntID(videoCodecID));
		
		videoDecoder.setWidth(videoWidth);
		videoDecoder.setHeight(videoHeight);
		//videoDecoder.setPixelFormat(videoFormat);
		
		return videoDecoder;
	}
	
	public Decoder getAudioDecoder()
	{
		Decoder audioDecoder = Decoder.make(Codec.findDecodingCodecByIntID(audioCodecID));
		
		audioDecoder.setSampleRate(audioRate);
		//audioDecoder.setChannels(audioChannels);
		audioDecoder.setChannelLayout(audioLayout);
		//audioDecoder.setSampleFormat(audioFormat);
		
		return audioDecoder;
	}
}
