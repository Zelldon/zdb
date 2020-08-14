# Zeebe Debug and Inspection tool

This repository contains an cli tool to inspect the internal state and log of a Zeebe partition. It is a Java (11) cli application and can be build via maven.
It was created during the Camunda Summer Hackdays in 2020.

## Table Of Contents

* [What problem does it solve](#what-problem-does-it-solve)
* [How does it solve it](#how-does-it-solve-it)
  * [State Inspection](#state-inspection)
    * [Inspect Zeebe Partition Status](#inspect-zeebe-partition-status)
    * [Inspect Incidents](#inspect-incidents)
    * [Inspect Blacklisted Workflow Instances](#inspect-blacklisted-workflow-instances)
    * [Inspect Workflows](#inspect-workflows)
  * [Log Inspection](#log-inspection)
    * [Inspect Log Status](#inspect-log-status)
    * [Inspect Log Consistency](#inspect-log-consistency)
    * [Inspect Log](#inspect-log)
 * [Autocompletion](#autocompletion)
 * [Testing](#testing)

## What problem does it solve

When Zeebe is broken there is currently no possibility to find out the last state of Zeebe.
If there was no exporter configured or they haven't exported for a while it get even worse, since it is not clear what the internal engine state is.

In order to shade some more light in the dark we build a tool called zdb - Zeebe Debuger. It should help you along the way during incidents and broken systems.

## How does it solve it

Using `zdb` you can inspect the internal state or the partition log.

### State Inspection

Using `zdb` you can inspect the internal `runtime` data or a snapshot.
It shows some information about the current state, incidents, workflows and so on from a single partition.
To inspect the database you should provide the path to the `raft-partition/partitions/../runtime/` folder in a partition or one of the snapshot folders `raft-partition/partitions/../snapshot/<snapshot-folder>`

You then can run several commands to inspect the given state.

#### Inspect Zeebe Partition Status

Shows the general information of a Zeebe partition.

```sh
zdb status --path=<pathToDatabase>
```

#### Inspect incidents

You can inspect incidents using the following commands.

List all incidents in this partition:

```sh
zdb incident list --path=<pathToDatabase>
```

Returns details to a specific incident:

```sh
zdb incident entity <IncidentKey> --path=<pathToDatabase>
```

#### Inspect Blacklisted Workflow Instances

You can check if there are any workflows stuck due to blacklisting using the following commands.

List all blacklisted workflow instances in this partition:

```sh
zdb blacklist list --path=<pathToDatabase>
```

Returns details to a specific blacklisted instance:

```sh
zdb blacklist entity <WorkflowInstanceKey> --path=<pathToDatabase>
```

#### Inspect Workflows
You can inspect all deployed workflows and get the resources of a specific workflow.

List all deployed workflows in this partition:

```sh
zdb workflow list --path=<pathToDatabase>
```

Returns details to a specific workflow:
```sh
zdb workflow entity <WorkflowKey> --path=<pathToDatabase>
```

### Log Inspection

You can also inspect the log stream using the command `zdb log` and his subcommands.
To inspect the log you should provide the path to a specific partition `raft-partition/partitions/<partition-id>/`.

#### Inspect Log Status

Shows the general information of a Zeebe partition log, e. g. how many indexes, max. entry size, avg. entry size etc.

```sh
zdb log status --path=<pathToPartition>
```

#### Inspect Log Consistency

The `zdb` cli provides the possibility to check the log for consistency. In order to do that use the following subcommand:

```sh
zdb log consistency --path=<pathToPartition>
```

It will search the log and verifies invariants, e. g. all indexes are increased by 1 etc.

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

## Examples

```sh
> zdb status --path=data/raft-partition/partitions/1/runtime/

Last processed position: 47248515375
Lowest exported position: No exporters
Blacklisted instances: Yes
Incidents: Yes
```

```sh
> zdb incident list --path=data/raft-partition/partitions/1/runtime/

Incident[key: 2251799813685269, workflow-instance-key: 2251799813685264, BPMN-process-id: "variable-mappings-workflow", error-type: IO_MAPPING_ERROR]
Incident[key: 2251799813685276, workflow-instance-key: 2251799813685271, BPMN-process-id: "variable-mappings-workflow", error-type: IO_MAPPING_ERROR]
Incident[key: 2251799813685567, workflow-instance-key: 2251799813685560, BPMN-process-id: "failing-job-workflow", error-type: UNHANDLED_ERROR_EVENT]
Incident[key: 2251799813685575, workflow-instance-key: 2251799813685568, BPMN-process-id: "failing-job-workflow", error-type: JOB_NO_RETRIES]
```

```sh
> zdb log status --path=data/raft-partition/partitions/1/
Scan log...
Log scanned in 1177 ms

Scanned entries: 726
Maximum entry size: 3873646
Minimum entry size: 10
Avg entry size: 88123.79201101928
LowestRecordPosition: 4294967296
HighestRecordPosition: 47248515296
HighestIndex: 726
LowestIndex: 1
InitialEntries: [InitializeEntry{term=1, timestamp=2020-07-29 10:59:18,416}, InitializeEntry{term=2, timestamp=2020-08-07 10:13:02,953}, InitializeEntry{term=3, timestamp=2020-08-07 10:49:02,330}, InitializeEntry{term=0, timestamp=1970-01-01 01:00:00,004}]
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

## Testing
As a hackday project, we've kept testing simple. Just execute the following in the root folder:

```sh
./test.sh
```
