package edu.cmu.pandaa.module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.MultiFrameStream;


public class ConstructGeometryModule implements StreamModule {
  GeometryHeader gHeader;
  DistanceHeader[] distanceHeaders;

  public ConstructGeometryModule()
  {
  }

  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof DistanceHeader)) {
      throw new IllegalArgumentException("Input multiheader should contain Distance Headers");
    }

    distanceHeaders = (DistanceHeader[]) multiHeader.getHeaders(new DistanceHeader[multiHeader.size()]);

    for (int j = 1; j < distanceHeaders.length; j++) {
      if(distanceHeaders[0].frameTime != distanceHeaders[j].frameTime)
        throw new IllegalArgumentException("Frame duration must be equal for all input frames");
    }

    /*generate unique device id array*/
    String[] deviceIds = generateDeviceIds(distanceHeaders);

    gHeader = new GeometryHeader(deviceIds, distanceHeaders[0].startTime, distanceHeaders[0].frameTime);
    return gHeader;
  }

  public String[] generateDeviceIds(DistanceHeader[] distanceHeaders)
  {
    int D = (int) Math.sqrt(1-(4)*(1)*(-2*distanceHeaders.length)); //D = sqrt(b*b - 4ac)    
    int numDevices = (D + 1)/2;
    
    String[] tempDeviceidBuffer = new String[2*(numDevices-1)];
    for(int i=0;i<numDevices-1;i++){
      for(int j=0;j<2;j++) //j<2 coz each DistanceHeader has only 2 elements
        tempDeviceidBuffer[2*i+j] = distanceHeaders[i].deviceIds[j];
    }

    Set<String> set = new HashSet<String>();
    for(int i=0; i < tempDeviceidBuffer.length; i++){
      if(set.contains(tempDeviceidBuffer[i])){
      } else {
        set.add(tempDeviceidBuffer[i]);
      }
    }

    return set.toArray(new String[0]);
  }
  
  public StreamFrame process(StreamFrame inFrame) {
    StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
    for(int i=0;i<frames.length;i++){
      if (!(frames[i] instanceof DistanceFrame)) {
        throw new IllegalArgumentException("Input multiframe should contain DistanceFrames");
      }
    }
    /*if (frames.length != 2) {
          throw new IllegalArgumentException("Input multiframe should contain two elements");
        }*/

    DistanceFrame[] dfIn = Arrays.copyOf(frames, frames.length, DistanceFrame[].class);
    
    int D = (int) Math.sqrt(1-(4)*(1)*(-2*frames.length)); //D = sqrt(b*b - 4ac)    
    int numDevices = (D + 1)/2;
    double[][] distanceMatrix = new double[numDevices][numDevices];
    int count = 0;
    for(int i=0;i<numDevices;i++){
    	for(int j=0;j<numDevices;j++){
    		if(i == j){
    			distanceMatrix[i][j] = 0.0;
    		}
    		else if(j < i)
    			distanceMatrix[i][j] = -1 * distanceMatrix[j][i];
    		else
    			distanceMatrix[i][j] = dfIn[count++].peakDeltas[0];
    	}
    }            
    GeometryFrame gfOut = gHeader.makeFrame(dfIn[0].seqNum, distanceMatrix);
    return gfOut;    
  }

  public void close() {
  }

  public static void main(String[] args) throws Exception {
    int i = 0;
    int argLen = args.length;
    String[] inArg = new String[argLen-1];
    String outArg = args[i];
    for(i=0;i<argLen-1;i++){
      inArg[i] = args[i+1];
    }
    if (i != argLen-1)
      throw new IllegalArgumentException("Invalid number of arguments");

    System.out.print("Combine "+(argLen - 1)+" pairwise distances: ");
    for(i=0;i<argLen-1;i++){
      System.out.print((i > 0 ? ", " : "") + inArg[i]);
    }
    System.out.println(" to "+outArg);

    FileStream[] ifs = new DistanceFileStream[argLen-1];

    for(i=0;i<argLen-1;i++){
      ifs[i] = new DistanceFileStream(inArg[i]);
    }

    MultiFrameStream mfs = new MultiFrameStream("tdoa123");

    for(i=0;i<argLen-1;i++){
      mfs.setHeader(ifs[i].getHeader());
    }

    FileStream ofs = new GeometryFileStream(outArg, true);

    ConstructGeometryModule ppd = new ConstructGeometryModule();
    ofs.setHeader(ppd.init(mfs.getHeader()));

    try {
    	
    	//while(true){
    	  for(i=0;i<argLen-1;i++){
          mfs.sendFrame(ifs[i].recvFrame());
        }
        //if (!mfs.isReady())
         // break;
        ofs.sendFrame(ppd.process(mfs.recvFrame()));
      //}
    } catch (Exception e) {
      e.printStackTrace();
    }
    ofs.close();
  }
}
