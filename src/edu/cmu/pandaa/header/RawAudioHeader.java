package edu.cmu.pandaa.header;

import java.io.Serializable;

import edu.cmu.pandaa.utils.DataConversionUtil;

/* 
 * RawAudioFormat to capture the WAV file format
 * Summary of data fields in WAV file
 * The canonical WAVE format starts with the RIFF header:
 *
 * 0         4   ChunkID          RIFF
 * 4         4   ChunkSize        36 + SubChunk2Size
 * 8         4   Format           WAVE
 * 
 * The "WAVE" format consists of two subchunks: "fmt " and "data":
 * The "fmt " subchunk describes the sound data's format:
 * 12        4   Subchunk1ID      "fmt "
 * 16        4   Subchunk1Size    16 for PCM. 
 * 20        2   AudioFormat      PCM = 1 
 * 22        2   NumChannels      Mono = 1, Stereo = 2, etc.
 * 24        4   SampleRate       8000, 44100, etc.
 * 28        4   ByteRate         == SampleRate * NumChannels * BitsPerSample/8
 * 32        2   BlockAlign       == NumChannels * BitsPerSample/8
 * 34        2   BitsPerSample    8 bits = 8, 16 bits = 16, etc.
 * 
 * The "data" subchunk contains the size of the data and the actual sound:
 * 36        4   Subchunk2ID      "data"
 * 40        4   Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
 * 44        *   Data             The actual sound data.
 */

public class RawAudioHeader extends StreamHeader implements Serializable {
	private int samplingRate;
	private int numChannels;
	private int audioFormat;
	private int bitsPerSample;
  private short[] derrive_save;
  private int[] smooth_save;
  private short[] prev, stage;
  private int prev_seqNum, stage_seqNum = -1;
  public String comment;

	public static final int DEFAULT_FRAMETIME = 100;
	public static final int WAV_FILE_HEADER_LENGTH = 44;

	public RawAudioHeader(String id, long startTime, int frameTime) {
		super(id, startTime, frameTime);
		// ideally fill in fields with some reasonable default -- this constructor
		// is for simple testing
	}

  public RawAudioHeader(String id, long startTime, int frameTime, int audioFormat,
      int numChannels, int samplingRate, int bitsPerSample) {
    this(id, startTime, frameTime, audioFormat, numChannels, samplingRate, bitsPerSample, null);
  }

  public RawAudioHeader(String id, long startTime, int frameTime, int audioFormat,
      int numChannels, int samplingRate, int bitsPerSample, String comment) {
		super(id, startTime, frameTime);
		this.samplingRate = samplingRate;
		this.numChannels = numChannels;
		this.audioFormat = audioFormat;
		this.bitsPerSample = bitsPerSample;
    this.comment = comment;
	}

	public long getSamplingRate() {
		return samplingRate;
	}

	public long getNumChannels() {
		return numChannels;
	}

	public int getAudioFormat() {
		return audioFormat;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public String toString() {
		return new String("Device ID: " + id + "\nSampling rate: " + samplingRate + "\nChannels: "
				+ numChannels + "\nAudio Format: " + audioFormat + "\nBits per sample: " + bitsPerSample);
	}

	public class RawAudioFrame extends StreamFrame implements Serializable {
		public short[] audioData;
    int dindex = 0;

    public RawAudioFrame(int frameLength) {
      audioData = new short[frameLength];
    }

    public RawAudioFrame(int frameLength, int seqNum) {
      super(seqNum);
      audioData = new short[frameLength];
    }

    public RawAudioFrame(RawAudioFrame clone) {
      super(clone.seqNum);
      audioData = clone.audioData.clone();
      dindex = clone.dindex;
    }

		public short[] getAudioData() {
			return audioData;
		}

		public String toString() {
			String result = " Length = " + audioData.length + " Data: ";
			for (int i = 0; i < 100; i++) {
				result += DataConversionUtil.shortToUnsignedShortVal(audioData[i]) + " ";
			}
			return result;
		}

    private double getSampleSq(int offset) {
      double dat;
      if (offset < 0) {
        dat = prev == null ? 0.0 : prev[prev.length + offset];
      } else {
        dat = audioData[offset];
      }

      return dat * dat;
    }

    public double[] smooth(int window) {
      if (prev_seqNum != seqNum-1) {
        prev = stage;
        prev_seqNum = stage_seqNum;
        stage = this.audioData.clone();
        stage_seqNum = seqNum;
      }

      // first time through this will point back to us, but that's OK
      double[] data = new double[audioData.length];
      double avg = 0;
      for (int i = 0;i < window; i++) {
        avg += getSampleSq(i-window);
      }
      for (int i = 0; i < data.length; i++) {
        avg -= getSampleSq(i-window);
        avg += getSampleSq(i);
        data[i] = Math.sqrt(avg / window);
      }
      return data;
    }

    public void smooth(boolean rms) {
      if (smooth_save == null || smooth_save.length == 0)
        return;
      short[] data = getAudioData();
      int size = smooth_save.length;
      for (int i = 0; i < data.length; i++) {
        int slot = i+seqNum*data.length;
        smooth_save[slot % size] = data[i] * (rms ? data[i] : 1);   // square if rms
        double sum = 0;
        for (int j = 0; j < size; j++) {
          sum += smooth_save[j];
        }
        double avg = rms ? Math.sqrt(sum/size) : sum/size;
        avg *= 5; // scale up to take advantage of short range
        avg -= Short.MIN_VALUE;
        if (avg >= Short.MAX_VALUE-1)
          avg = Short.MIN_VALUE-1;
        data[i] = (short) avg;  // rms = sqrt(mean_square)
      }
    }

    public void derrive() {
      short[] data = getAudioData();
      short prev = derrive_save[dindex];
      for (int i = 0; i < data.length; i++) {
        short save = data[i];
        data[i] = (short) (save - prev);
        prev = save;
      }
      derrive_save[dindex++] = prev;
    }

    public RawAudioFrame clone() {
      return new RawAudioFrame(this);
    }
  }

  public void initFilters(int win, int der) {
    smooth_save = new int[(int) getSamplingRate()*win/22050];
    derrive_save = new short[der];
  }

  public RawAudioFrame makeFrame(int frameLength, int seqNum) {
    return new RawAudioFrame(frameLength, seqNum);
  }

  public RawAudioFrame makeFrame(int frameLength) {
    return new RawAudioFrame(frameLength);
  }

  public RawAudioFrame makeFrame() {
    return new RawAudioFrame(getFrameSize());
  }

  public int getFrameSize() {
    if ((frameTime * samplingRate) % 1000 != 0) {
      throw new RuntimeException("Losing data in getFrameSize calculation");
    }
    return (frameTime * samplingRate)/1000;
  }
}
