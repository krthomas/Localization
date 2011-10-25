package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.DummyStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import mdsj.*;

class ProcessGeometryModule implements StreamModule{
	FrameStream inGeometryStream, outGeometryStream;
	GeometryHeader hOut;	

  public ProcessGeometryModule(String[] args) throws Exception
  {
	  if (args.length == 0) {
		  throw new RuntimeException("No arguments provided");
	    } else {
	      int count = 1;
	      for (String file : args) {
	        FrameStream in = new DummyStream(new GeometryHeader("dummy" + count++, System.currentTimeMillis(), frameTime));
	        activateNewDevice(in);
	      }
	    }
  }
  
  
  public void runModule(FrameStream inGeometryStream, FrameStream outGeometryStream) {
	  try{
		  StreamHeader header = init(inGeometryStream.getHeader());
		  outGeometryStream.setHeader(header);
		  StreamFrame frameIn,frameOut;
		  while ((frameIn = inGeometryStream.recvFrame()) != null) {
			  frameOut = process(frameIn);
		      outGeometryStream.sendFrame(frameOut);
		  }
	  }catch(Exception e){
		  e.printStackTrace();
	  }
	  close();
  }

  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof GeometryHeader))
      throw new RuntimeException("Wrong header type");
    
    /*compute new header*/
    GeometryHeader hIn = (GeometryHeader)inHeader ;
   	hOut = new GeometryHeader(hIn.deviceIds, hIn.startTime, hIn.frameTime);
    return hOut;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (!(inFrame instanceof GeometryFrame))
      throw new RuntimeException("Wrong frame type");
    
    GeometryFrame gfIn = (GeometryFrame) inFrame ;
    GeometryFrame gfOut = hOut.makeFrame(gfIn.seqNum, gfIn.geometry); //TODO:verify correctness of hOut    
    gfOut.geometry = MDSJ.classicalScaling(gfIn.geometry); // apply MDS
	return gfOut ;    
  }
  
  public static void main(String[] args) throws Exception
  {
	  //GeometryFileStream gIn = new GeometryFileStream(args[1]);
	  //GeometryFileStream gOut = new GeometryFileStream(args[2]);
	  // = new ProcessGeometryModule();
	   
	  
	  try {
		  ProcessGeometryModule pgm = new ProcessGeometryModule(args);
		  pgm.runModule();
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
  
  
  public void close() {
  }
}