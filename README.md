# Zeebe Debug and Inspection tool

## What problem does it solve

When Zeebe is broken there is currently no possibility to find out the last state of Zeebe.
If there was no exporter configured or they haven't exported for a while it get even worse, since it is not clear what the internal engine state is.

In order to shade some more light in the dark we build a tool called zdb - Zeebe Debuger. It should help you along the way during incidents and broken systems.

## How does it solve it

Using `zdb` you can inspect the internal `runtime` data or a snapshot.
It shows some information about the current state, incidents, workflows and so on from a single partition.
To inspect the database you should provide the path to the `raft-partition/partitions/../runtime/` folder in a partition or one of the snapshot folders `raft-partition/partitions/../snapshot/<snapshot-folder>`

You can also inspect the log stream using the command `zdb log`.
To inspect the log you should provide the path to the partition `raft-partition/partitions/<partition-id>/`


### Zeebe Status

Shows the last status of a Zeebe partition.

```sh
zdb status --path=<pathToDatabase>
```

### Inspect incidents

You can inspect incidents using the following commands.

```sh
zdb incident list --path=<pathToDatabase>
```

```sh
zdb incident entity <IncidentKey> --path=<pathToDatabase>
```

### Blacklisted workflow instances

You can check if there are any workflows stuck due to blacklisting using the following commands.

```sh
zdb blacklist list --path=<pathToDatabase>
```

```sh
zdb blacklist entity <WorkflowInstanceKey> --path=<pathToDatabase>
```

### Workflows

List all deployed Workflows

```sh
zdb workflow list --path=<pathToDatabase>
```

Show one workflow
```sh
zdb workflow entity <WorkflowKey> --path=<pathToDatabase>
```

### Inpect log

```sh
zdb log scan --path=<pathToPartition>
```
