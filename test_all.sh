JAR=Localization.jar
CLASSPATH=$PWD/`find . -name $JAR`

if [ ! -f $CLASSPATH ]; then
  echo Could not find $JAR
  exit
fi

CLASSPATH=$CLASSPATH:$PWD/lib/mdsj.jar
echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH" #use with Linux
#OPTS="-classpath `cygpath -wp $CLASSPATH`" #use with Cygwin ons Windows
PACKAGE=edu.cmu.pandaa

cd test/

# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

java $OPTS $PACKAGE.stream.RawAudioFileStream mangled_audio.wav sample_input-1.wav
java $OPTS $PACKAGE.module.ImpulseStreamModule impulses-1.txt sample_input-1.wav 
java $OPTS $PACKAGE.module.ConsolidateModule i 1-1 impulses-c.txt impulses-1.txt 
java $OPTS $PACKAGE.module.TDOACorrelationModule distances.txt impulses-1.txt impulses-2.txt 
java $OPTS $PACKAGE.module.ConsolidateModule d 1-1 distance-c.txt distance-1.txt 
java $OPTS $PACKAGE.module.ConstructGeometryModule geometry1234.txt distance12.txt distance13.txt distance14.txt distance23.txt distance24.txt distance34.txt
java $OPTS $PACKAGE.module.GeometryMatrixModule geometryOut.txt geometryIn.txt

