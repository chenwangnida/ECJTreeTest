 #!/bin/sh

need sgegrid

NUM_RUNS=40

qsub -t 1-$NUM_RUNS:1 webservice_gp.sh ~/workspace/swscowlstc/Set04MetaData owlstc-singlejob-gp4;
