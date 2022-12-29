# Data Flow

This document describes the data flow of the application.

## Table of Content

* [Visual Flow Chart](#visual-flow-chart): Visual Flow Chart.

## Visual Flow Chart

```mermaid
flowchart LR;
    TES{Trading Engine Starts} ---> |kafka-consumer| BA
    TES{Trading Engine Starts} ---> |http| EXM
    
    subgraph BA[Bitstamp Adaptor]
        TP1[Trading Pair 1] -.- TPN[Trading Pair... N]
    end
    
   subgraph EXM[Exchange Messages]
        TP3[Trading Pair 1] -.- TP5[Trading Pair... N]
    end
    
    BA --> MAE
    EXM --> MAE
    subgraph MAE[Matching Engine]
    end
    
    MAE --> AUT
    subgraph AUT[Audit Trail]
    end
    
    AUT --> |Deserialize| KP
    subgraph KP[Kafka Producer]
    end
```
