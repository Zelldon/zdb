# Zeebe Debug and Inspection tool

## What Problem solves it

When Zeebe is broken there is currently no possibilty to find out the last state of Zeebe. 
If there was no exporter configured or they haven't exported for a while it get even worse, since it is not clear what the internal engine state is.

In order to shade some more light in the dark we build a tool called zdb - Zeebe Debuger. It should help you along the way during incidents and broken systems.

## How does it solve it

### Zeebe Status

Shows the last status of a Zeebe partition.

```sh
zdb status --path=<pathToPartition>
```
