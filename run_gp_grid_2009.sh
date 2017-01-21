 #!/bin/sh

need sgegrid

NUM_RUNS=40

for i in {1..5}; do
  qsub -t 1-$NUM_RUNS:1 webservice_gp.sh ~/workspace/swsc2009/Set0${i}MetaData 2009-semantic-200-30${i};
done