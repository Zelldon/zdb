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
    * [Print Log](#print-log)
 * [Autocompletion](#autocompletion)
 * [Testing](#testing)

## What problem does it solve

When Zeebe is broken there is currently no possibility to find out the last state of Zeebe.
If there was no exporter configured or they haven't exported for a while it get even worse, since it is not clear what the internal engine state is.

In order to shed some more light in the dark we build a tool called zdb - Zeebe Debugger. It should help you along the way during incidents and broken systems.

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

#### Print Log

It is possible to print the complete log to standard out. This is can be quite helpful if you want to track down some records, which might have caused some issues.

To print the log:

```sh
zdb log print --path=<pathToPartition>
```

To pipe it to a file:

```sh
zdb log print --path=<pathToPartition> > output.log
```
The `output.log` file will contain all records as json. Zeebe entries are written as json, RAFT entries unfortunately not.

## Examples

```sh
> zdb status --path=data/raft-partition/partitions/1/runtime/

Processing: 
	Last processed position: 141169121
Exporting: 
	elasticsearch: position 141169060
	MetricsExporter: position 141169122
	Lowest exported position: 141169060
Incident related:: 
	Blacklisted instances: 0
	Incidents: 33
Messages: : 3656965
	Current Time: : 1610561195140
	Message next deadline: : 1610556566223
	Message last deadline: : 1615738151675
WorkflowInstances: : 17
	ElementInstances: : 34
Variables: 158
	min size: 1
	max size: 963
	avg size: 27.158228
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
Log scanned in 147 ms
Meta:
	Last voted for: 0
	Persisted term: 3028
Configuration: Configuration{index=0, time=2020-11-13 11:41:30,995, members=[DefaultRaftMember{id=2, type=ACTIVE, updated=2020-11-13T10:41:30.995318Z}, DefaultRaftMember{id=1, type=ACTIVE, updated=2020-11-13T10:41:30.995318Z}, DefaultRaftMember{id=0, type=ACTIVE, updated=2020-11-13T10:41:30.995318Z}]}

Scanned entries: 7489
Maximum entry size: 11848
Minimum entry size: 14
Avg entry size: 773.4973961810656
LowestRecordPosition: 204608746
HighestRecordPosition: 204621183
HighestIndex: 123186388
LowestIndex: 123178900
InitialEntries: [Indexed{index=123186138, entry=InitializeEntry{term=2971, timestamp=2021-02-10 03:15:59,934}}, Indexed{index=123186139, entry=InitializeEntry{term=2973, timestamp=2021-02-10 03:18:56,289}}, Indexed{index=123186140, entry=InitializeEntry{term=2975, timestamp=2021-02-10 03:19:09,022}}, Indexed{index=123186141, entry=InitializeEntry{term=2977, timestamp=2021-02-10 03:22:09,518}}, Indexed{index=123186142, entry=InitializeEntry{term=2979, timestamp=2021-02-10 03:22:22,408}}, Indexed{index=123186143, entry=InitializeEntry{term=2981, timestamp=2021-02-10 03:24:38,394}}, Indexed{index=123186144, entry=InitializeEntry{term=2983, timestamp=2021-02-10 03:27:30,906}}, Indexed{index=123186145, entry=InitializeEntry{term=2984, timestamp=2021-02-10 03:31:03,999}}, Indexed{index=123186295, entry=InitializeEntry{term=2972, timestamp=2021-02-10 03:18:51,044}}, Indexed{index=123186297, entry=InitializeEntry{term=2974, timestamp=2021-02-10 03:19:01,392}}, Indexed{index=123186298, entry=InitializeEntry{term=2976, timestamp=2021-02-10 03:22:03,033}}, Indexed{index=123186299, entry=InitializeEntry{term=2978, timestamp=2021-02-10 03:22:14,059}}, Indexed{index=123186300, entry=InitializeEntry{term=2980, timestamp=2021-02-10 03:24:20,144}}, Indexed{index=123186301, entry=InitializeEntry{term=2982, timestamp=2021-02-10 03:26:48,999}}, Indexed{index=123186302, entry=InitializeEntry{term=2985, timestamp=2021-02-10 03:38:03,701}}, Indexed{index=123186303, entry=InitializeEntry{term=2987, timestamp=2021-02-10 03:40:38,517}}, Indexed{index=123186304, entry=InitializeEntry{term=2989, timestamp=2021-02-10 03:42:07,966}}, Indexed{index=123186305, entry=InitializeEntry{term=2991, timestamp=2021-02-10 03:44:47,562}}, Indexed{index=123186306, entry=InitializeEntry{term=2993, timestamp=2021-02-10 03:46:54,454}}, Indexed{index=123186307, entry=InitializeEntry{term=2995, timestamp=2021-02-10 03:49:20,204}}, Indexed{index=123186308, entry=InitializeEntry{term=2997, timestamp=2021-02-10 03:52:26,666}}, Indexed{index=123186309, entry=InitializeEntry{term=2999, timestamp=2021-02-10 03:55:19,408}}, Indexed{index=123186310, entry=InitializeEntry{term=3001, timestamp=2021-02-10 03:57:26,688}}, Indexed{index=123186311, entry=InitializeEntry{term=3003, timestamp=2021-02-10 03:58:48,385}}, Indexed{index=123186312, entry=InitializeEntry{term=3005, timestamp=2021-02-10 04:01:16,230}}, Indexed{index=123186313, entry=InitializeEntry{term=3007, timestamp=2021-02-10 04:03:07,766}}, Indexed{index=123186314, entry=InitializeEntry{term=3009, timestamp=2021-02-10 04:05:49,928}}, Indexed{index=123186315, entry=InitializeEntry{term=3011, timestamp=2021-02-10 04:07:32,429}}, Indexed{index=123186316, entry=InitializeEntry{term=3013, timestamp=2021-02-10 04:10:10,244}}, Indexed{index=123186317, entry=InitializeEntry{term=3015, timestamp=2021-02-10 04:11:50,673}}, Indexed{index=123186318, entry=InitializeEntry{term=3017, timestamp=2021-02-10 04:13:17,654}}, Indexed{index=123186319, entry=InitializeEntry{term=3019, timestamp=2021-02-10 04:14:40,159}}, Indexed{index=123186320, entry=InitializeEntry{term=3021, timestamp=2021-02-10 04:16:03,563}}, Indexed{index=123186321, entry=InitializeEntry{term=3023, timestamp=2021-02-10 04:16:15,409}}, Indexed{index=123186334, entry=InitializeEntry{term=3024, timestamp=2021-02-10 05:07:36,169}}, Indexed{index=123186346, entry=InitializeEntry{term=3025, timestamp=2021-02-10 07:53:34,548}}, Indexed{index=123186359, entry=InitializeEntry{term=3026, timestamp=2021-02-10 08:48:12,638}}, Indexed{index=123186371, entry=InitializeEntry{term=3027, timestamp=2021-02-11 01:41:24,250}}, Indexed{index=123186372, entry=InitializeEntry{term=3028, timestamp=2021-02-11 01:42:43,666}}]
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
