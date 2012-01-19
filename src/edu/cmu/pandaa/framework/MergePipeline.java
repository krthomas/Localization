package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.DistanceMatrixModule;
import edu.cmu.pandaa.module.GeometryMatrixModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:12 PM
 */

public class MergePipeline implements StreamModule {

  StreamModule matrix = new DistanceMatrixModule();
  StreamModule geometry = new GeometryMatrixModule();
  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof DistanceHeader)) {
       throw new IllegalArgumentException("Merge pipe multiheader should contain ImpulseHeaders");
    }

    StreamHeader header = matrix.init(inHeader);
    header = geometry.init(header);

    if (!(header instanceof GeometryHeader)) {
      throw new IllegalArgumentException("Output should be GeometryHeader");
    }

    trace = new GeometryFileStream(inHeader.id + ".txt", true, true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = matrix.process(inFrame);
    trace.sendFrame(frame);
    StreamFrame geomOut = geometry.process(frame);

    return geomOut;
  }

  @Override
  public void close() {
    trace.close();
  }}
