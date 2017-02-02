 #!/bin/sh

need sgegrid

NUM_RUNS=2

  qsub -t 1-$NUM_RUNS:1 newscript.sh ~/workspace/swscowlstc/Set01MetaData owlstc-newscript-gp1;
