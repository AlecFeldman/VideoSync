import io.humble.video.Decoder;
import io.humble.video.Codec;
import io.humble.video.AudioFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;

import java.io.Serializable;

import io.humble.video.AudioChannel.Layout;

public class SerializedStream implements Serializable
{
	private int videoIndex;
	private int videoCodecID;
	private int videoFlags;
	private int videoFlagsTwo;
	private int videoWidth;
	private int videoHeight;
	private int videoNumerator;
	private int videoDenominator;
	
	private int audioIndex;
	private int audioCodecID;
	private int audioFlags;
	private int audioFlagsTwo;
	private int audioRate;
	private int audioChannels;
	private int audioNumerator;
	private int audioDenominator;
	
	private static final long serialVersionUID = 1L;
	
	private PixelFormat.Type videoFormat;
	private AudioFormat.Type audioFormat;
	
	private Layout audioLayout;
	
	public SerializedStream(int videoIndex, int audioIndex, Decoder videoDecoder, Decoder audioDecoder)
	{
		this.videoIndex = videoIndex;
		videoCodecID = videoDecoder.getCodec().getIDAsInt();
		videoFlags = videoDecoder.getFlags();
		videoFlagsTwo = videoDecoder.getFlags2();
		videoWidth = videoDecoder.getWidth();
		videoHeight = videoDecoder.getHeight();
		videoNumerator = videoDecoder.getTimeBase().getNumerator();
		videoDenominator = videoDecoder.getTimeBase().getDenominator();
		videoFormat = videoDecoder.getPixelFormat();
		
		this.audioIndex = audioIndex;
		audioCodecID = audioDecoder.getCodec().getIDAsInt();
		audioFlags = audioDecoder.getFlags();
		audioFlagsTwo = audioDecoder.getFlags2();
		audioRate = audioDecoder.getSampleRate();
		audioChannels = audioDecoder.getChannels();
		audioNumerator = audioDecoder.getTimeBase().getNumerator();
		audioDenominator = audioDecoder.getTimeBase().getDenominator();
		audioLayout = audioDecoder.getChannelLayout();
		audioFormat = audioDecoder.getSampleFormat();
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
		
		videoDecoder.setFlags(videoFlags);
		videoDecoder.setFlags2(videoFlagsTwo);
		videoDecoder.setWidth(videoWidth);
		videoDecoder.setHeight(videoHeight);
		videoDecoder.setPixelFormat(videoFormat);
		videoDecoder.setTimeBase(Rational.make(videoNumerator, videoDenominator));
		
		return videoDecoder;
	}
	
	public Decoder getAudioDecoder()
	{
		Decoder audioDecoder = Decoder.make(Codec.findDecodingCodecByIntID(audioCodecID));
		
		audioDecoder.setFlags(audioFlags);
		audioDecoder.setFlags2(audioFlagsTwo);
		audioDecoder.setSampleRate(audioRate);
		audioDecoder.setChannels(audioChannels);
		audioDecoder.setChannelLayout(audioLayout);
		audioDecoder.setSampleFormat(audioFormat);
		audioDecoder.setTimeBase(Rational.make(audioNumerator, audioDenominator));
		
		return audioDecoder;
	}
}
