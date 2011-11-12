package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public class GeometryFileStream extends FileStream {
  private GeometryHeader header;

  public GeometryFileStream(String filename) throws Exception {
    super(filename);
  }

  public GeometryFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  public void setHeader(StreamHeader h) throws Exception {
    GeometryHeader header = (GeometryHeader) h;
    String tempId="";
    for(int i=0;i<header.deviceIds.length;i++){
      tempId += header.deviceIds[i];
      tempId+=",";
    }
    tempId = tempId.substring(0,tempId.length()-1);
    writeString(tempId + " " + header.startTime + " " + header.frameTime + " " + header.rows + " " + header.cols);
  }

  public void sendFrame(StreamFrame f) throws Exception {
    GeometryFrame frame = (GeometryFrame) f;
    int rows = frame.geometry.length;
    int cols = frame.geometry[0].length;
    String msg = "" + frame.seqNum;
    for (int i = 0;i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double val = frame.geometry[i][j];
        // simple way to keep the numbers reasonable (not too much precision)
        // really ony to make it visually look better...
        val = Math.floor(val*100.0)/100.0;
        msg += " " + val;
      }
      msg += "   ";
    }
    writeString(msg.trim());
  }

  public GeometryHeader getHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    String[] deviceIds = parts[0].split(","); //extract the array of deviceIds
    header = new GeometryHeader(deviceIds,
            Long.parseLong(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]));
    return header;
  }

  public GeometryFrame recvFrame() throws Exception {
    String line = readLine();
    if (line == null)
      return null;
    String[] parts = line.split(" ");
    int seqNum = Integer.parseInt(parts[0]);
    int rows = header.rows;
    int cols = header.cols;
    double[][] geometry = new double[rows][cols];
    int pos = 1; // skip leading sequence num
    for (int i = 0;i < rows;i++) {
      for (int j = 0; j < cols; j++) {
        while (pos < parts.length && parts[pos].trim().equals(""))
          pos++;
        geometry[i][j] = Double.parseDouble(parts[pos++]);
      }
    }
    return header.makeFrame(seqNum, geometry);
  }

  /*public static void main(String[] args) throws Exception {
    String filename = "test.txt";
    String[] deviceIds = {"1a","2b","3c","4d"};

    double[][] inputDissimilarity = {{0,1,2,3},
    								{4,5,6,7},
    								{8,9,10,11},
    								{12,13,14,15}};
    
    GeometryFileStream foo = new GeometryFileStream(filename, true);
    
    GeometryHeader header = new GeometryHeader(deviceIds, System.currentTimeMillis(), 100);
    foo.setHeader(header);
    GeometryFrame frame1 = header.makeFrame(inputDissimilarity);
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame(inputDissimilarity));
    foo.sendFrame(header.makeFrame(inputDissimilarity));
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new GeometryFileStream(filename);
    GeometryHeader header2 = foo.getHeader();
    GeometryFrame frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    foo.close();

    if (frame1.getHeader().startTime != frame2.getHeader().startTime) {
      System.err.println("Start time mismatch!");
    }
    if (frame1.seqNum != frame2.seqNum-2) {
      System.err.println("Sequence number mismatch!");
    }
  }*/
}