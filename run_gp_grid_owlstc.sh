 #!/bin/sh

need sgegrid

NUM_RUNS=40

for i in {1..5}; do
  qsub -t 1-$NUM_RUNS:1 webservice_gp.sh ~/workspace/swscowlstc/Set0${i}MetaData owlstc-semantic-gp${i};
done