# Summer 2024
## Researchers
Jayven Cachola and Penny Silliman

## Overview: 
Implemented tf-socket agent which uses TensorFlow DL networks to decide actions to take in Blind FSM.  It does not do Deep Q Learning at this point (July 2024).  See the paper submitted to ICAART 2025.

## Extant Branches from this work:
### adding-sensor-support
    Jayven began work to support non-goal sensors (e.g., odd/even) but we got stuck 
    because it's unclear how best to do predictions with the DL network.  Also, the 
    scope of this project exceeded our remaining time. So, it's currently incomplete.
### Tensorflow-layer-modifications
    Penny used this branch to experiment with different DL network designs.  
    No need to merge.
### egreedy-tuning
    We added e-Greedy to the agent.  We will likely merge this later this summer 
### truncation-modification
    Jayven experimented with an alternative approach to truncating predicted action 
    sequences. See Journal for details.  This had no significant effect so it was not merged.
