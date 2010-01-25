********************************************************************************
    INSTRUCTIONS ON RUNNING A TRANCHE SERVER ON A LINUX OPERATING SYSTEM
********************************************************************************


PREREQUISITES: Install Java 5 or later version


1. UNZIP THE DOWNLOADED TRANCHE SERVER ZIP FILE.


2. NAVIGATE TO THE TOP-MOST DIRECTORY IN THE UNZIPPED FILE.


3. CHECK THAT PORT 443 IS OPEN. IF IT IS NOT, YOUR TRANCHE SERVER WILL NOT COMMUNICATE WITH THE OUTSIDE WORLD.


4. RUN THE FOLOWING COMMAND: sudo chmod +x run.sh


5. RUN THE FOLLOWING COMMAND: sudo nohup ./run.sh > tranche.out 2> tranche.err


YOUR LOCAL COMPUTER IS NOW A TRANCHE SERVER ON THE PROTEOMECOMMONS.ORG TRANCHE REPOSITORY.