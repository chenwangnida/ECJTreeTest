 #!/bin/sh

need sgegrid

NUM_RUNS=40

qsub -t 1-$NUM_RUNS:1 webservice_gp.sh ~/workspace/swscowlstc/Set03MetaData owlstc-singlejob-gp3;
