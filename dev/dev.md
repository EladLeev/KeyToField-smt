# Development Environment

## DB Connection
Username: `sample`  
Password: `sample`  
DB Name: `sample`

## Create a new connector without the SMT
```bash
curl -X "POST" "http://localhost:8083/connectors" \
     -H "Content-Type: application/json" \
     -d $'{
  "name": "jdbc-source",
  "config": {
    "connector.class": "JdbcSourceConnector",
    "tasks.max": "1",
    "connection.url":"jdbc:postgresql://db/sample?user=sample&password=sample",
    "mode": "bulk",
    "table.whitelist":"actor",
    "validate.non.null":"false",
    "topic.prefix":"postgres-jdbc-",
    "name": "jdbc-source",
    "transforms": "createKey,extractInt",
    "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
    "transforms.createKey.fields":"actor_id",
    "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
    "transforms.extractInt.field":"actor_id"
  }
}'
```

## Create a new connector with the SMT
```bash
curl -X "POST" "http://localhost:8083/connectors" \
     -H "Content-Type: application/json" \
     -d $'{
  "name": "jdbc-source",
  "config": {
    "connector.class": "JdbcSourceConnector",
    "tasks.max": "1",
    "connection.url":"jdbc:postgresql://db/sample?user=sample&password=sample",
    "mode": "timestamp",
    "timestamp.column.name":"last_update",
    "table.whitelist":"actor",
    "validate.non.null":"false",
    "topic.prefix":"postgres-jdbc-",
    "name": "jdbc-source",
    "transforms": "createKey,extractInt,keyToField",
    "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
    "transforms.createKey.fields":"actor_id",
    "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
    "transforms.extractInt.field":"actor_id",
    "transforms.keyToField.type": "com.github.eladleev.kafka.connect.transform.keytofield.KeyToFieldTransform",
    "transforms.keyToField.field.name":"primaryKey",
    "transforms.keyToField.field.delimiter": "_"
  }
}'
```

## Known Issues
* `mujz/pagila` is not working well on Apple Silicon.  
Make sure to use latest Docker version, and set "Use Rosetta for x86/amd64 emulation on Apple Silicon" to true.  
If that's not possible, uncomment to use `synthesizedio/pagila` image instead.
