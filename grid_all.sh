#!/bin/sh

# First argument is the parameter file e.g. DGPoas.params
# Second argument is the output directory e.g. dgpoas
NUM_JOBS=35

# Call the grid
need sgegrid

DATE=`date +%Y`

DIR_GRID="/vol/grid-solar/sgeusers/parkjohn/"
DIR_YEAR="${DIR_GRID}${DATE}/"
DIR_RAW="${DIR_YEAR}raw_rules/"

DIR_GRID_HOME="/u/students/parkjohn/phd_research/grid/"
DIR_RULE="${DIR_GRID_HOME}${2}/"

# Update the jar files, if needed.
$DIR_GRID_HOME/update_jars.sh

# Create the output directory, if it doesn't exist yet.
if [ ! -d $DIR_RULE ]; then
    mkdir $DIR_RULE
fi
SUBDIR_PREFIX=`date +%F-%k%M | 
    sed -r 's/-/\n/g' | 
    tac | 
    tr '\n' '-' | 
    sed -r 's/.$//g' | 
    sed 's/ //g'`

# Make the subdirectories for the results to come into.
DIR_OUTPUT="${DIR_RULE}${SUBDIR_PREFIX}/"

echo $1
echo $DIR_OUTPUT
echo $NUM_JOBS

mkdir $DIR_OUTPUT

# Run the grid each dataset.
i=1
while [ $i -le $NUM_JOBS ]; do
    qsub grid_jss.sh $1 $DIR_OUTPUT $NUM_JOBS $i
    i=`expr $i + 1`
done