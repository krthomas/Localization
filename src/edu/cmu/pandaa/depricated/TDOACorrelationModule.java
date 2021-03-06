package edu.cmu.pandaa.depricated;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.MultiFrameStream;

public class TDOACorrelationModule implements StreamModule {
  DistanceHeader header;
  final int maxAbsDistance = 29 * 1000;   // max plausible distance between peaks of the same event (micro-seconds)

  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof ImpulseHeader)) {
      throw new IllegalArgumentException("Input multiheader should contain ImpulseHeaders");
    }

    StreamHeader[] impulseHeaders = multiHeader.getHeaders();
    if (impulseHeaders.length != 2) {
      throw new IllegalArgumentException("Input multiheader should contain two elements");
    }

    if (impulseHeaders[0].frameTime != impulseHeaders[1].frameTime) {
      throw new IllegalArgumentException("Frame duration must be equal for both input frames");
    }

    String[] deviceIds = new String[] {impulseHeaders[0].id, impulseHeaders[1].id};

    header = new DistanceHeader(inHeader.id, impulseHeaders[0].startTime, impulseHeaders[0].frameTime, deviceIds);
    return header;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
    if (frames.length != 2) {
      throw new IllegalArgumentException("Input multiframe should contain two elements");
    }
    for (int i = 0;i < frames.length;i++) {
      if (frames[i] != null && !(frames[i] instanceof ImpulseFrame)) {
        throw new IllegalArgumentException("Input multiframe should contain ImpulseFrames");
      }
    }

    /* Try to match peaks in the two frames.
    * We're assuming that the same sound impulse reaches devices
    * within less than 29ms (speed of sound over 10m, a generous
    * room size). Tweaking may be needed.
    */
    ImpulseFrame aFrame = (ImpulseFrame) frames[0];
    ImpulseFrame bFrame = (ImpulseFrame) frames[1];
    
    // if any of the frames has no impulses, then there's nothing to be matched
    if (aFrame == null || aFrame.peakOffsets.length == 0 || bFrame == null || bFrame.peakOffsets.length == 0) {
      return header.makeFrame(new double[] {}, new double[] {});
    }
    
    // all potential peak matches
    LinkedList<int[]> potentialMatches = new LinkedList<int[]>();
    // list of [<index of potential match>, <absolute distance>], sorted by <absolute distance>
    TreeSet<int[]> sortedAbsDistances = new TreeSet<int[]>(new ArrayComparator(1));
        
    int aIndex = 0, bIndex = 0, higherOffset, absDistance;
    
    while (aIndex < aFrame.peakOffsets.length && bIndex < bFrame.peakOffsets.length) {
      // get the higher value of the two peaks' offsets
      higherOffset = Math.max(aFrame.peakOffsets[aIndex], bFrame.peakOffsets[bIndex]);
      
      // for the peak with the higher offset, see if there is a correspondent that is closer to it
      while (aIndex + 1 < aFrame.peakOffsets.length && aFrame.peakOffsets[aIndex + 1] < higherOffset) {
        aIndex++;
      }
      while (bIndex + 1 < bFrame.peakOffsets.length && bFrame.peakOffsets[bIndex + 1] < higherOffset) {
        bIndex++;
      }
      
      // if the peak pair is of a plausible distance, add it to the list of potential matches
      absDistance = Math.abs(aFrame.peakOffsets[aIndex] - bFrame.peakOffsets[bIndex]);
      if (absDistance < maxAbsDistance) {
        potentialMatches.add(new int[] {aIndex, bIndex});
        sortedAbsDistances.add(new int[] {potentialMatches.size() - 1, absDistance});
      }
      
      // advance the peak pointer with the lower offset (will jump past the one with the higher offset)
      if (aFrame.peakOffsets[aIndex] > bFrame.peakOffsets[bIndex]) {
        bIndex++;
      } else {
        aIndex++;
      }
    }
    
    // accept peak matches in decreasing order of distance (every peak can only be matched once)
    HashSet<Integer> aIndexesVisited = new HashSet<Integer>();
    HashSet<Integer> bIndexesVisited = new HashSet<Integer>();
    TreeSet<int[]> matchAbsDistances = new TreeSet<int[]>(new ArrayComparator(0));
    int[] potentialMatch;
    for (int[] dist : sortedAbsDistances) {
      potentialMatch = potentialMatches.get(dist[0]);
      if (!aIndexesVisited.contains(potentialMatch[0]) && !bIndexesVisited.contains(potentialMatch[1])) {
        aIndexesVisited.add(potentialMatch[0]);
        bIndexesVisited.add(potentialMatch[1]);
        matchAbsDistances.add(dist);
      }
    }
    
    // compute distances and magnitudes of matched peaks
    LinkedList<Double> matchDeltas = new LinkedList<Double>();
    LinkedList<Double> matchMagnitudes = new LinkedList<Double>();
    int[] match;
    for (int[] dist : matchAbsDistances) {
      match = potentialMatches.get(dist[0]);
      matchDeltas.add((double) aFrame.peakOffsets[match[0]] - bFrame.peakOffsets[match[1]]);
      matchMagnitudes.add((double) (aFrame.peakMagnitudes[match[0]] * bFrame.peakMagnitudes[match[1]]));
    }
    
    // convert data to double[] to be stored in the frame
    int size = matchDeltas.size();
    double[] peakDeltas = new double[size];
    double[] peakMagnitudes = new double[size];
    for (int i = 0; i < size; i++) {
      peakDeltas[i] = matchDeltas.get(i);
      peakMagnitudes[i] = matchMagnitudes.get(i);
    }
    
    return header.makeFrame(peakDeltas, peakMagnitudes);
  }

  public void close() {

  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String outf = args[arg++];
    String in1 = args[arg++];
    String in2 = args[arg++];
    if (arg != args.length)
      throw new IllegalArgumentException("Excess arguments");

    System.out.println("TDOACorrelation: " + outf + " " + in1 + " " + in2);
    FileStream ifs1 = new ImpulseFileStream(in1);
    FileStream ifs2 = new ImpulseFileStream(in2);

    MultiFrameStream mfs = new MultiFrameStream("tdoa");
    mfs.setHeader(ifs1.getHeader());
    mfs.setHeader(ifs2.getHeader());

    FileStream ofs = new DistanceFileStream(outf, true);

    TDOACorrelationModule tdoa = new TDOACorrelationModule();
    ofs.setHeader(tdoa.init(mfs.getHeader()));

    try {
      mfs.noblock = true;
      while (true) {
        mfs.sendFrame(ifs1.recvFrame());
        mfs.sendFrame(ifs2.recvFrame());
        ofs.sendFrame(tdoa.process(mfs.recvFrame()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    ofs.close();
  }

  static class ArrayComparator implements Comparator<int[]> {
    private int index;
    ArrayComparator(int index) {
      this.index = index;
    }
    public int compare(int[] a, int[] b) {
      return (a[index] > b[index]) ? 1 : -1;
    }
  }
}
