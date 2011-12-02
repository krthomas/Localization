package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.*;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.*;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:11 PM
 */

public class DualPipeline implements StreamModule {
  /* fileds only used for dummy distance header -- remove when we have real code */
  final long now = System.currentTimeMillis();
  final int frameTime = 100;

  /* First step is to take in a multiFrame consisting of impulseframes and turn it into time differences */
  StreamModule tdoa = new TDOACorrelationModule();

  /* Then we consolidate bunches of impulsdummyIdse frames into larger consolidated frames for processing */
  StreamModule smooth = new ConsolidateModule('d', 1, 1, 1, 1);

  //  StreamModule smooth = new DummyModule(new DistanceHeader("smoother", now, frameTime, new String[] {"1", "2"}));

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof ImpulseHeader)) {
       throw new IllegalArgumentException("Dual pipe multiheader should contain ImpulseHeaders");
    }
    if (multiHeader.getHeaders().length != 2) {
      throw new IllegalArgumentException("Dual pipe multiheader should contain two elements");
    }

    StreamHeader header = tdoa.init(inHeader);
    header = smooth.init(header);

    if (!(header instanceof DistanceHeader)) {
      throw new IllegalArgumentException("Output should be DistanceHeader");
    }

    trace = new DistanceFileStream(inHeader.id + ".txt", true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = tdoa.process(inFrame);
    frame = smooth.process(frame);
    trace.sendFrame(frame);
    return frame;
  }

  @Override
  public void close() {
    trace.close();
  }
}
