# KeyToField - SMT for Kafka Connect / Debezium
A Kafka Connect SMT that allows you to add the record key to the value as a named field.

## Overview
The `KeyToField` transformation is designed to enhance Kafka Connect functionality by including the record key as a field within the record's value. This can be particularly useful in scenarios where downstream systems require access to the original key alongside the record data.

> This SMT was featured on [Confluent's Newsletter](https://developer.confluent.io/newsletter/you-put-what-in-your-events/#:~:text=A%20single%20message%20key%20to%20field%20transform%20for%20Kafka%20Connect/Debezium%20by%20Elad%20Leev!%20In%20a%20way%20it%E2%80%99s%20the%20reverse%20of%20the%20ValueToKey%20SMT%20that%20comes%20with%20Kafka%20Connect%2C%20useful%20for%20when%20the%20fields%20in%20the%20key%20are%20not%20included%20in%20the%20value.)! 🚀

## Features
* Add the record key to the value as a named field.
* Customizable field name and delimiter.

## Installation
1. Use the latest release on GitHub, or build the JAR file from source using Maven:
```bash
mvn clean package
```
2. Copy the generated JAR file (`keytofield-transform-<version>.jar`) to the Kafka Connect plugins directory.
3. Restart Kafka Connect for the reload the plugin directory.
4. Update your connector with the SMT configuration

## Configuration
The KeyToField transformation can be configured with the following properties:

* `field.name`: Name of the field to insert the Kafka key to (default: `kafkaKey`).
* `field.delimiter`: Delimiter to use when concatenating the key fields (default: `-`).

## Usage
To use the `KeyToField` transformation, add it to your Kafka Connect connector configuration:
```
transforms=keyToField
transforms.keyToField.type=com.github.eladleev.kafka.connect.transform.keytofield.KeyToFieldTransform
transforms.keyToField.field.name=primaryKey
transforms.keyToField.field.delimiter=_
```

### Example
Consider a Kafka topic with the following record:


```json
{
  "key": {
    "id": 123,
    "timestamp": 1644439200000
  },
  "value": {
    "data": "example"
  }
}
```

After applying the `KeyToField` transformation, the record will be transformed as follows:

```json
{
  "key": {
    "id": 123,
    "timestamp": 1644439200000
  },
  "value": {
    "data": "example",
    "primaryKey": "123_1644439200000"
  }
}
```
## Local Development
For your convenience, under `dev/` you can find a `docker-compose` file that contains all necessary components for local development and testing. Kafka Connect will automatically load the connector from the `target/` directory.   
Use the attached bash script to submit a new Kafka Connect connector. `Adminer` can be used to ingest new data to the database, reflected by an event in Kafka.

## Contributing
Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details of submitting a pull requests.

## License
This project is licensed under the Apache License - see the [LICENSE](LICENSE) file for details.
