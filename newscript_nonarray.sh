#!/bin/sh
#
# Force Bourne Shell if not Sun Grid Engine default shell (you never know!)
#
#$ -S /bin/sh
#
# I know I have a directory here so I'll use it as my initial working directory
#
#$ -wd /vol/grid-solar/sgeusers/sawczualex
#
# End of the setup directives
#
# Now let's do something useful, but first change into the job-specific directory that should
#  have been created for us
#
# Check we have somewhere to work now and if we don't, exit nicely.
#

if [ -d /local/tmp/wangchen/$JOB_ID.$SGE_TASK_ID ]; then
        cd /local/tmp/wangchen/$JOB_ID.$SGE_TASK_ID
else
        echo "Uh oh ! There's no job directory to change into "
        echo "Something is broken. I should inform the programmers"
        echo "Save some information that may be of use to them"
        echo "Here's LOCAL TMP "
        ls -la /local/tmp
        echo "AND LOCAL TMP SAWCZUALEX "
        ls -la /local/tmp/wangchen
        echo "Exiting"
        exit 1
fi

#
# Now we are in the job-specific directory so now can do something useful
#
# Stdout from programs and shell echos will go into the file
#    scriptname.o$JOB_ID
#  so we'll put a few things in there to help us see what went on
#

echo ==UNAME==
uname -n
echo ==WHO AM I and GROUPS==
id
groups
echo ==SGE_O_WORKDIR==
echo $SGE_O_WORKDIR
echo ==/LOCAL/TMP==
ls -ltr /local/tmp/
echo ==/VOL/GRID-SOLAR==
ls -l /vol/grid-solar/sgeusers/

#
# OK, where are we starting from and what's the environment we're in
#
echo ==RUN HOME==
pwd
ls
echo ==ENV==
env
echo ==SET==
set
#
echo == WHATS IN LOCAL/TMP ON THE MACHINE WE ARE RUNNING ON ==
ls -ltra /local/tmp | tail
#
echo == WHATS IN LOCAL TMP FRED JOB_ID AT THE START==
ls -la

# -----------------------------------

#
# Initialise path variables
#

#DIR_HOME="/u/students/sawczualex/"
DIR_HOME="/home/wangchen/"
DIR_GRID="/vol/grid-solar/sgeusers/wangchen/"
DIR_WORKSPACE="workspace/"
DIR_PROGRAM=$DIR_HOME$DIR_WORKSPACE"ECJUnfoldTreeTest/"
ECJ_JAR=$DIR_HOME$"lib2/*"
DIR_OUTPUT=$DIR_GRID$"owlstc-newscript-gp1" # Name of directory containing output

FILE_JOB_LIST="CURRENT_JOBS.txt"
FILE_RESULT_PREFIX="out"
ANALYSIS_PREFIX="eval"
FILE_FRONT_PREFIX="front"


#
# Copy the input files to the local directory
#

cp -r $DIR_PROGRAM"bin" .
cp $DIR_PROGRAM"wsc.params" .
cp $ECJ_JAR .
cp ~"/workspace/swscowlstc/Set01MetaData/"* . # Copy datasets

echo ==WHATS THERE HAVING COPIED STUFF OVER AS INPUT==
ls -la

#
# Run the program
#
seed=$SGE_TASK_ID
result=$FILE_RESULT_PREFIX$seed.stat

#seed=$SGE_TASK_ID
#result=$FILE_RESULT_PREFIX$seed.stat
#analysis=$ANALYSIS_PREFIX$seed.stat
#front=$FILE_FRONT_PREFIX$seed.stat

#java -cp ecj.23.jar:./bin:. ec.Evolve -file $3 -p seed.0=$seed -p stat.file=\$$result -p stat.evaluations=\$$analysis -p stat.front=\$$front
java -cp jgraph-5.13.0.0.jar:jgrapht-core-1.0.1.jar:guava-20.0.jar:ecj.23.jar:./bin:. wsc.ecj.gp.Evolve -file wsc.params -p seed.0=$seed -p stat.file=\$$result

echo ==AND NOW, HAVING DONE SOMTHING USEFUL AND CREATED SOME OUTPUT==
ls -la

# Now we move the output to a place to pick it up from later
cd results
if [ ! -d $DIR_OUTPUT ]; then
  mkdir $DIR_OUTPUT
fi
cp *.stat $DIR_OUTPUT

echo "Ran through OK"


