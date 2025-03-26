package com.github.eladleev.kafka.connect.transform.keytofield;

import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.apache.kafka.connect.transforms.util.SchemaUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public class KeyToFieldTransform<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String OVERVIEW_DOC = "Add the record key to the value as a named field.";
    private static final int DEFAULT_CACHE_SIZE = 16;

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define("field.name", ConfigDef.Type.STRING, "kafkaKey", ConfigDef.Importance.HIGH,
                    "Name of the field to insert the Kafka key to")
            .define("field.delimiter", ConfigDef.Type.STRING, "-", ConfigDef.Importance.LOW,
                    "Delimiter to use when concatenating the key fields")
            .define("schema.cache.size", ConfigDef.Type.INT, DEFAULT_CACHE_SIZE, ConfigDef.Importance.LOW,
                "Size of the schema update cache");

    private static final String PURPOSE = "adding key to record";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyToFieldTransform.class);
    private String fieldName;
    private String fieldDelimiter;
    private Cache<Schema, Schema> schemaUpdateCache;

    @Override
    public void configure(Map<String, ?> props) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);
        fieldName = config.getString("field.name");
        fieldDelimiter = config.getString("field.delimiter");
        int schemaCacheSize = config.getInt("schema.cache.size");
        schemaUpdateCache = new SynchronizedCache<>(new LRUCache<>(schemaCacheSize));
    }

    @Override
    public R apply(R record) {
        if (record.valueSchema() == null) {
            return applySchemaless(record);
        } else {
            return applyWithSchema(record);
        }
    }

    private R applySchemaless(R record) {
        LOGGER.trace("Applying SMT without a value schema");
        LOGGER.trace("Record key: {}", record.key());
        final Map<String, Object> value;
        if (record.value() == null) {
            value = new HashMap<>(1);
        } else {
            value = requireMap(record.value(), PURPOSE);
        }

        final String keyAsString = extractKeyAsString(record.keySchema(), record.key());
        value.put(fieldName, keyAsString);

        LOGGER.trace("Key as string: {}", keyAsString);
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                record.valueSchema(),
                value,
                record.timestamp()
        );
    }

    private R applyWithSchema(R record) {
        LOGGER.trace("Applying SMT with a schema");
        LOGGER.trace("Record key: {}", record.key());

        final Struct value = requireStruct(record.value(), PURPOSE);


        Schema updatedSchema = schemaUpdateCache.get(value.schema());
        LOGGER.trace("Updated schema: {}", updatedSchema);
        if (updatedSchema == null) {
            updatedSchema = makeUpdatedSchema(value.schema());
            LOGGER.trace("Schema NULL updatedSchema: {}", updatedSchema.fields());
            schemaUpdateCache.put(value.schema(), updatedSchema);
        }

        final Struct updatedValue = new Struct(updatedSchema);
        LOGGER.trace("Updated value: {}", updatedValue);

        for (Field field : value.schema().fields()) {
            updatedValue.put(field.name(), value.get(field));
        }
        LOGGER.trace("Updated value after fields: {}", updatedValue);


        final String keyAsString = extractKeyAsString(record.keySchema(), record.key());

        updatedValue.put(fieldName, keyAsString);

        LOGGER.trace("Key as string: {}\nNew schema:{}", keyAsString, updatedValue.schema().fields());
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                updatedSchema,
                updatedValue,
                record.timestamp()
        );
    }

    private String extractKeyAsString(Schema schema, Object key) {
        LOGGER.trace("Extracting key as string");


        if (!(key instanceof Struct)) {
            return key.toString();
        }

        Struct keyStruct = (Struct) key;
        StringBuilder keyAsStringBuilder = new StringBuilder();

        // Call a recursive method to handle nested structures
        buildKeyString(schema, keyStruct, keyAsStringBuilder);

        // Remove the last field delimiter if the key is not empty and the last character is the delimiter
        int length = keyAsStringBuilder.length();
        while (length > 0 && keyAsStringBuilder.charAt(length - 1) == fieldDelimiter.charAt(0)) {
            keyAsStringBuilder.setLength(--length);
        }

        LOGGER.trace("Key as string: {}", keyAsStringBuilder.toString());
        return keyAsStringBuilder.toString();
    }

    private void buildKeyString(Schema schema, Struct keyStruct, StringBuilder keyAsStringBuilder) {
        for (Field field : schema.fields()) {
            if (field.schema().type() == Schema.Type.STRUCT) {
                buildKeyString(field.schema(), keyStruct.getStruct(field.name()), keyAsStringBuilder);
                keyAsStringBuilder.append(fieldDelimiter);
            } else {
                Object fieldValue = keyStruct.get(field);

                if (fieldValue != null) {
                    LOGGER.trace("field name: " + field.name());
                    keyAsStringBuilder.append(fieldValue).append(fieldDelimiter);
                }
            }
        }
    }


    private Schema makeUpdatedSchema(Schema schema) {
        LOGGER.trace("build the updated schema");
        SchemaBuilder newSchemabuilder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());
        for (org.apache.kafka.connect.data.Field field : schema.fields()) {
            newSchemabuilder.field(field.name(), field.schema());
        }

        LOGGER.trace("adding the new field: {}", fieldName);
        newSchemabuilder.field(fieldName, Schema.OPTIONAL_STRING_SCHEMA);
        return newSchemabuilder.build();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        schemaUpdateCache = null;
    }
}
