#!/bin/bash

# TODO comments explaining the arguments here.

DIR_TMP="/local/tmp/parkjohn/$JOB_ID/"
DIR_HOME="/u/students/parkjohn/"
DIR_PHD="${DIR_HOME}phd_research/"
DIR_GRID="${DIR_PHD}grid/"
DIR_REPO="${DIR_PHD}repositories/jss.git/JSS_Research/"
DIR_OUTPUT=$2

DIR_LIB="${DIR_REPO}lib/"
DIR_JAR="${DIR_REPO}output_jars/"
DIR_DATA="${DIR_REPO}dataset/"

FILE_JAR="jss_evolve.jar"
FILE_RESULT_PREFIX=`echo $FILE_JAR | cut -d '.' -f 1`
FILE_ERROR="grid_jss.sh.e${JOB_ID}"

RANDOM=26556

mkdir -p $DIR_TMP

# Preliminary test to ensure that the directory has been created successfully.
if [ ! -d $DIR_TMP ]; then
    echo "Could not create the temporary directory for processing the job. "
    echo "/local/tmp/ directory: "
    ls -la /local/tmp
    echo "/local/tmp/parkjohn directory: "
    ls -la /local/tmp/parkjohn
    echo "Exiting"
    exit 1
fi

# Copy the files required for processing into the temporary directory.
cp -r ${DIR_REPO}evolve_params $DIR_TMP
cp -r ${DIR_DATA} $DIR_TMP
cp $DIR_LIB* $DIR_TMP
cp ${DIR_JAR}${FILE_JAR} $DIR_TMP
mkdir -p ${DIR_TMP}results

# Note that we need the full path to this utility, as it is not on the PATH
#need java2-native
#need sysdirs
# /usr/pkg/java/jdk-1.6.0/bin/java -jar PSOfs1.jar
#!/bin/sh

i=1
j=0
m=$4

# Skip the first n-1 partitions
n=`expr 40 / $3`
echo "Number of partitions: ${n}, number of jobs in each partition: ${3}"

echo "Skipping: "
while [ $i -lt $m ] 
do
    while [ $j -lt $n ] 
    do 
        seed=$RANDOM
        echo "Skipping ${j}th seed (${seed}) of ${i}th partition"
        j=`expr $j + 1`
    done
    j=0
    i=`expr $i + 1`
done

cd $DIR_TMP

# Run the main JSS program
echo "Running $1: "
while [ $j -lt $n ] 
do
    seed=$RANDOM
    result="${FILE_RESULT_PREFIX}${seed}.txt"
    front="${FILE_RESULT_PREFIX}${seed}_front.txt"
    time="${FILE_RESULT_PREFIX}${seed}_time.txt"

    echo "Running ${j}th seed of (${seed}) of ${i}th partition"
    echo "Current directory: `pwd`"

    time java -jar "$FILE_JAR" -file evolve_params/$1 -p seed.0=$seed -p stat.file=\$$result stat.front=\$$front
    echo "Output file: `readlink -f "${result}"`"

    sed -n '/real/,$p' ${DIR_HOME}${FILE_ERROR} > $time

    cp ${DIR_TMP}${result} ${DIR_TMP}results
    cp ${DIR_TMP}${time} ${DIR_TMP}results
    j=`expr $j + 1`
done

# Now we move the output to a place to pick it up from later and clean up
# (really should check that directory exists too, but this is just a test)
cd "results"
echo "Files in path `pwd`"
ls -l
cp ${DIR_TMP}results/*.txt $DIR_OUTPUT

# Do the cleaning up from our starting directory
rm -rf /local/tmp/parkjohn/$JOB_ID

# Finish the job.
echo "Ran through OK"