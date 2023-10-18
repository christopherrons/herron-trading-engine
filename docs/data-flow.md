# Data Flow

This document describes the data flow of the application.

## Table of Content

* [Visual Flow Chart](#visual-flow-chart): Visual Flow Chart.

## Visual Flow Chart

```mermaid
flowchart LR;
TES{Trading Engine Starts} --->|broadcast|BA
TES{Trading Engine Starts} ---> |broadcast|EXM

subgraph BA[User Generated Order Data]
BSP1[Partition 1] -.- BSPN[Partition.. N]
end

subgraph EXM[Reference Data]
EXP1[Partition 1] -.- EXP1N[Partition... N]
end

BA -->|Events|TAE
EXM -->|Events|TAE
subgraph TAE[Trading Engine]
end

TAE -->|Events|MAE
subgraph MAE[Matching Engine]
MAEP1[Partition 1] -.- MAEPN[Partition... N]
end

MAE -->|Orderbook Events|AUT
subgraph AUT[Audit Trail]
AUTP1[Partition 1] -.- AUTPN[Partition... N]
end

MAE -->|Price Quotes|TOB
subgraph TOB[Top of Book - Price Quotes]
TOBP1[Partition 1] -.- TOBPN[Partition... N]
end

MAE -->|Trades|TR
subgraph TR[Trades]
TRP1[Partition 1] -.- TRPN[Partition... N]
end

AUT -->|Deserialize|KP
TR -->|Deserialize|KP
TOB -->|Deserialize|KP
subgraph KP[Kafka Producer]
KPTP1[Partition 1] -.- KPTPN[Partition... N]
end
```
