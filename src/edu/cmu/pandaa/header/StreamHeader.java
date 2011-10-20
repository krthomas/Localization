package edu.cmu.pandaa.header;

import java.io.Serializable;

public class StreamHeader implements Serializable {
  public final String id; // device ID (hostname, IP address, whatever)
  public final long startTime; // client start time, ala System.currentTimeMillis()
  public final int frameTime;  // duration of each frame, measured in ms
  private int nextSeq; // next sequence number to use by frame constructor

  public StreamHeader(String id, long startTime, int frameTime) {
    this.id = id;
    this.startTime = startTime;
    this.frameTime = frameTime;
  }

  public class StreamFrame implements Serializable {
    public final int seqNum;

    public StreamFrame() {
      seqNum = nextSeq++;
    }

    public StreamFrame(int seq) {
      seqNum = seq;
    }

    public StreamHeader getHeader() {
      return StreamHeader.this;
    }
  }

  public StreamFrame makeFrame() {
    return new StreamFrame();
  }
}
