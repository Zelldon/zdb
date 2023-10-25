# Zeebe Debug and Inspection tool

This repository contains a cli tool to inspect the internal state and log of a Zeebe partition. It is a Java (17) cli application and can be build via maven.
It was kicked off during the Camunda Summer Hackdays in 2020 and has been maintained and developed since then.

The following Zeebe versions are supported:

* 8.1
* 8.2
* 8.3
* SNAPSHOT

## Table Of Contents

* [What problem does it solve](#what-problem-does-it-solve)
* [Usage](#usage)
* [How does it solve it](#how-does-it-solve-it)
  * [State Inspection](#state-inspection)
    * [Inspect Zeebe Partition Status](#inspect-zeebe-partition-status)
    * [Inspect Incidents](#inspect-incidents)
    * [Inspect Processes](#inspect-processes)
    * [Inspect Instances](#inspect-instances)
  * [Log Inspection](#log-inspection)
    * [Inspect Log Status](#inspect-log-status)
    * [Inspect Log Consistency](#inspect-log-consistency)
    * [Inspect Log](#inspect-log)
    * [Print Log](#print-log)
 * [Autocompletion](#autocompletion)

## What problem does it solve

When Zeebe is broken there is currently no possibility to find out the last state of Zeebe.
If there was no exporter configured or they haven't exported for a while it gets even worse, since it is not clear what the internal engine state is.

To shed some more light in the dark we build a tool called zdb - Zeebe Debugger. It should help you along the way during incidents and broken systems.


## Usage

> **Note:**
> To be on the safe side make sure to copy Zeebe data to a separate location, to not mess with a running Zeebe process and mistakingly corrupt any data.

### Docker

If you have copied data from Zeebe to your local machine you could run the following:

```
 docker run -v <path>/<partitionId>/:/<partitionId>/ ghcr.io/zelldon/zdb log print -p "/<partitionId>"
```

### Kubernetes

If have Zeebe installed in Kubernetes and want to investigate the Zeebe data you can run `zdb` as an [ephemeral container](https://kubernetes.io/docs/concepts/workloads/pods/ephemeral-containers/)

```
kubectl debug -it -c zdb --image=ghcr.io/zelldon/zdb:latest --attach=true --target=zeebe zeebe-0 -- /bin/bash
```

### Local CLI

Alternatively to the strategies above you can download the fat-jar and script and run that locally

```bash
cd /usr/bin
curl -O -L https://github.com/Zelldon/zdb/releases/latest/download/zdb
curl -O -L https://github.com/Zelldon/zdb/releases/latest/download/zdb.jar
sed -i 's/target\///' zdb
chmod u+x zdb
zdb --version
```

## How does it solve it

Using `zdb` you can inspect the internal state or the partition log.

### State Inspection

Using `zdb` you can inspect the internal `runtime` data or a snapshot.
It shows some information about the current state, incidents, processes, and so on from a single partition.
To inspect the database you should provide the path to the `raft-partition/partitions/../runtime/` folder in a partition or one of the snapshot folders `raft-partition/partitions/../snapshot/<snapshot-folder>`

You then can run several commands to inspect the given state.

#### Inspect Zeebe Partition Status

Shows the general information of a Zeebe partition. It will show you a statistic (counts) for each existing column family in the state.

```sh
zdb state --path=<pathToDatabase>
```

Furthermore, the complete state can be printed as json via the `list` sub-command.


```sh
zdb state list --path=<pathToDatabase>
```

This can be more fine-tuned and a specific column family can be given, such that only key-value pairs are printed to the console.

For example, to see all processes

```
$ zdb state --path=<pathToDatabase> list -cf PROCESS_CACHE
```

#### Inspect incidents

You can inspect incidents using the following commands.

List all incidents in this partition:

```sh
zdb incident list --path=<pathToDatabase>
```

Returns detail to a specific incident:

```sh
zdb incident entity <IncidentKey> --path=<pathToDatabase>
```

Find incidents for a given processInstanceKey

```sh
zdb incident list --path=<pathToDatabase> | jq '. | map(select(.processInstanceKey==<PI_KEY>))'
```

#### Inspect Banned Process Instances

You can check if there are any processes stuck due to banning using the following commands.

List all banned process instances in this partition:

```sh
zdb banned list --path=<pathToDatabase>
```

Returns details to a specific banned instance:

```sh
zdb banned entity <ProcessInstanceKey> --path=<pathToDatabase>
```

#### Inspect Processes
You can inspect all deployed processes and get the resources of a specific process.

List all deployed processes in this partition:

```sh
zdb process list --path=<pathToDatabase>
```

Returns details to a specific process:
```sh
zdb process entity <ProcessKey> --path=<pathToDatabase>
```

List all element instances for the given process:

```sh
zdb process --path=<pathToDatabase> instances <ProcessKey>
```

#### Inspect Instances

You can inspect existing element instances and get details viewed of there state.

Print all information to a given element instance:

```sh
zdb instance --path=<pathToDatabase> entity <elementInstanceKey>
```

#### Inspect state (generic)

There is a new (experimental) feature to inspect the state on a generic way with the `state` subcommand. You can either 
print the complete state as json or specify a specific column family (used in Zeebe).

Example to see all processes

```
$ zdb state --path=<pathToDatabase> list -cf PROCESS_CACHE
```

### Log Inspection

You can also inspect the log stream using the command `zdb log` and his subcommands.
To inspect the log you should provide the path to a specific partition `raft-partition/partitions/<partition-id>/`.

#### Inspect Log Status

Shows the general information of a Zeebe partition log, e. g. how many indexes, max. entry size, avg. entry size etc.

```sh
zdb log status --path=<pathToPartition>
```

#### Inspect Log

It is possible to inspect the log in more detail and search for a specific index **OR** position.

To search for a record position use:

```sh
zdb log search --path=<pathToPartition> --position=<position>
```
It will print all related information to the record, when it exists in the log.


To search for an index use:

```sh
zdb log search --path=<pathToPartition> --index=<position>
```

It will print a details to the specific index, when it exists in the log.

#### Print Log

It is possible to print the complete log to standard out. This can be quite helpful if you want to track down some records, which might have caused some issues. 

To print the log:

```sh
zdb log print --path=<pathToPartition>
```

Per default, the log is printed in JSON format.
To pipe it to a file:

```sh
zdb log print --path=<pathToPartition> > output.log
```
The `output.log` file will contain all records as JSON.

##### Limit

You can limit the printed log via the options `--to` and `--from`.

I you want to skip the first records or X positions you can use `zdb log print --path=<pathToPartition> --from X` whereas X must be a long.

For defining a limit of the print (like until which position the log should be printed) you can use `--to` like this `zdb log print --path=<pathToPartition> --to X` whereas X must be a long.

##### Filter

An interesting use case is to print only certain records, for example for only specific process instances. 

You can filter the printed log via: `--instanceKey`

```sh
 zdb log print -p <pathToPartition> --instanceKey 2251799813686738
```

##### Format

We support different formats to print the log, like json, table or dot. The json format is used per default. Can be set via `-f` or `--format`

**Table**

```
zdb log print --format TABLE --path=<pathToPartition>
```

The `table` format will print the complete log as space separated table. This can be consumed by other csv tools.

Example:

```sh
Index Term Position SourceRecordPosition Timestamp Key RecordType ValueType Intent ProcessInstanceKey BPMNElementType 
836304301 304 6888891257 6888891180 1692869671126 2251802375814765 COMMAND PROCESS_INSTANCE ACTIVATE_ELEMENT 2251802375814765 PROCESS 
836304301 304 6888891258 6888891180 1692869671126 2251802375814770 EVENT PROCESS_INSTANCE_CREATION CREATED 2251802375814765 
836304301 304 6888891259 6888891180 1692869671126 2251802375814765 EVENT PROCESS_INSTANCE ELEMENT_ACTIVATING 2251802375814765 PROCESS 
836304301 304 6888891260 6888891180 1692869671126 2251802375814765 EVENT PROCESS_INSTANCE ELEMENT_ACTIVATED 2251802375814765 PROCESS 
```

**Dot**

```
zdb log print -f dot -p=<pathToPartition>
```

The `dot` format will print the complete log as graph in [dot language](https://graphviz.org/doc/info/lang.html). This can be consumed by [graphviz](https://graphviz.org/doc/info/command.html) to generate a visual graph of the log.


Generate dot file via:
`zdb log print -d -p <pathToPartition> > output.dot`

Generate svg:
`dot -Tsvg -o test.svg test.dot`

![test](https://user-images.githubusercontent.com/2758593/156778874-1c1fb44a-e18c-4cac-b226-6052241ebdc8.svg)


## Examples

**Details of a specific column family**
```sh
zdb state list -p $PATH -cf EXPORTER | jq
{
  "data": [
    {
      "cf": "EXPORTER",
      "key": "00 00 00 00 00 00 00 28 00 00 00 0d 65 6c 61 73 74 69 63 73 65 61 72 63 68",
      "value": {
        "exporterPosition": 619675,
        "exporterMetadata": "eyJyZWNvcmRDb3VudGVyc0J5VmFsdWVUeXBlIjp7IkRFUExPWU1FTlQiOjE0LCJQUk9DRVNTX0lOU1RBTkNFIjo1ODcsIklOQ0lERU5UIjo1LCJNRVNTQUdFIjo3OSwiTUVTU0FHRV9TVUJTQ1JJUFRJT04iOjM5LCJQUk9DRVNTX01FU1NBR0VfU1VCU0NSSVBUSU9OIjoxNDksIk1FU1NBR0VfU1RBUlRfRVZFTlRfU1VCU0NSSVBUSU9OIjozNywiVkFSSUFCTEUiOjE5NywiUFJPQ0VTU19JTlNUQU5DRV9DUkVBVElPTiI6MSwiUFJPQ0VTUyI6MTMsIkNPTU1BTkRfRElTVFJJQlVUSU9OIjo4NH19"
      }
    },
    {
      "cf": "EXPORTER",
      "key": "00 00 00 00 00 00 00 28 00 00 00 0f 4d 65 74 72 69 63 73 45 78 70 6f 72 74 65 72",
      "value": {
        "exporterPosition": 619676,
        "exporterMetadata": ""
      }
    }
  ]
}
```

**Retrieve a process model**

```
$ zdb state --path=<pathToDatabase> list -cf PROCESS_CACHE | jq --raw-output '.data[0].value.resource' | base64 -d > model.bpmn
```

## Autocompletion
`zdb` comes with autocompletion. Just print it to a file:

```sh
zdb generate-completion >> ~/.autocompletions/zdb
```

and source that file in your shell profile (i.e. `.bash_rc`, `.zsh_rc`, `.bash_profile`, etc.):
```sh
source <(cat $HOME/.autocompletions/zdb)
```
