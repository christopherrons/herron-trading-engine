# Deploy Scripts

Once the application is deployed, script are used for starting, monitoring and stopping the application.

## Table of Content

* [Scripts](#scripts): Deploy scripts.

## Scripts

### Variables

Common variables used by the scripts are set in:

* [setenv-server.sh](../trading-engine-deploy/src/main/java/com/herron/exchange/tradingengine/deploy/scripts/setenv-server.sh)

### Bootstrap script

The [bootstrap.sh](../trading-engine-deploy/src/main/java/com/herron/exchange/tradingengine/deploy/scripts/bootstrap.sh)
is used to init and maintain the deploy directory.

### Start Script

Start the application by running:

1. [startup-server.sh](../trading-engine-deploy/src/main/java/com/herron/exchange/tradingengine/deploy/scripts/startup-server.sh).

### Shutdown Script

1. [shutdown-server.sh](../trading-engine-deploy/src/main/java/com/herron/exchange/tradingengine/deploy/scripts/shutdown-server.sh).

## Crontab

Crontab is used for
scheduled [task](../trading-engine-deploy/src/main/java/com/herron/exchange/tradingengine/deploy/cron/bitstamp-consumer.crontab).
