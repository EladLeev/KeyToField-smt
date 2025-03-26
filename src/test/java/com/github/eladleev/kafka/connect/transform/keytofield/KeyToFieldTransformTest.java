package com.github.eladleev.kafka.connect.transform.keytofield;

import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;


import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class KeyToFieldTransformTest {
    private KeyToFieldTransform<SourceRecord> xform = new KeyToFieldTransform<>();

    @After
    public void tearDown() throws Exception {
        xform.close();
    }

    @Test(expected = DataException.class)
    public void topLevelStructRequired() {
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));
        xform.apply(new SourceRecord(null, null, "", 0, Schema.INT32_SCHEMA, 42));
    }

    @Test
    public void applySchemalessTest() {
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));

        Schema keySchema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .build();

        Struct key = new Struct(keySchema)
                .put("id", 123);

        SourceRecord record = new SourceRecord(
                null, null, "test-topic", keySchema, key, null, null);

        SourceRecord transformedRecord = xform.apply(record);

        assertNotNull(transformedRecord);
        if (transformedRecord.value() != null && transformedRecord.value() instanceof Map) {
            Map<String, Object> transformedValue = (Map<String, Object>) transformedRecord.value();
            assertEquals(transformedValue.get("primaryKey"), "123");
        }
        else {
         fail("Transformed value is null or not an instance of Map");
        }
    }

    @Test
    public void applyNonDefaultDelimiterTest() {
        System.out.println("[Test] applyNonDefaultDelimiterTest");

        Map<String, String> configMap = new HashMap<>();
        configMap.put("field.name", "primaryKey");
        configMap.put("field.delimiter", "_");
        configMap.put("schema.cache.size", "16");
        xform.configure(configMap);

        Schema keySchema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("name", Schema.STRING_SCHEMA)
                .build();

        Struct key = new Struct(keySchema)
                .put("id", 123)
                .put("name", "John_Doe");

        Schema valueSchema = SchemaBuilder.struct()
                .field("valueField", Schema.STRING_SCHEMA)
                .build();

        Struct value = new Struct(valueSchema)
                .put("valueField", "someValue");

        SourceRecord record = new SourceRecord(
                null, null, "test-topic", 0, keySchema, key, valueSchema, value);

        SourceRecord transformedRecord = xform.apply(record);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof Struct);

        Struct transformedValue = (Struct) transformedRecord.value();
        assertEquals(transformedValue.get("primaryKey"), "123_John_Doe");
        assertEquals(transformedValue.get("valueField"), "someValue");
        System.out.println("[Test] PK: " + transformedValue.get("primaryKey"));
        System.out.println("[Test] applyNonDefaultDelimiterTest Done");
    }

    @Test
    public void applyWithSchemaTest() {
        System.out.println("[Test] applyWithSchemaTest");
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));

        Schema keySchema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .build();

        Struct key = new Struct(keySchema)
                .put("id", 123);

        Schema valueSchema = SchemaBuilder.struct()
                .field("valueField", Schema.STRING_SCHEMA)
                .build();

        Struct value = new Struct(valueSchema)
                .put("valueField", "someValue");

        SourceRecord record = new SourceRecord(
                null, null, "test-topic", 0, keySchema, key, valueSchema, value);

        SourceRecord transformedRecord = xform.apply(record);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof Struct);

        Struct transformedValue = (Struct) transformedRecord.value();
        assertEquals(transformedValue.get("primaryKey"), "123");
        assertEquals(transformedValue.get("valueField"), "someValue");
        System.out.println("[Test] PK: " + transformedValue.get("primaryKey"));
        System.out.println("[Test] applyWithSchemaTest Done");
    }

    @Test
    public void applyWithComplexSchemaTest() {
        System.out.println("[Test] applyWithComplexSchemaTest");
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));

        // Test with a bit more complex schema
        Schema keySchema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("details", SchemaBuilder.struct()
                        .field("name", Schema.STRING_SCHEMA)
                        .field("age", Schema.INT32_SCHEMA)
                        .build())
                .build();

        Struct key = new Struct(keySchema)
                .put("id", 123)
                .put("details", new Struct(keySchema.field("details").schema())
                        .put("name", "John_Doe")
                        .put("age", 30));

        Schema valueSchema = SchemaBuilder.struct()
                .field("valueField", Schema.STRING_SCHEMA)
                .build();

        Struct value = new Struct(valueSchema)
                .put("valueField", "someValue");

        SourceRecord record = new SourceRecord(
                null, null, "test-topic", 0, keySchema, key, valueSchema, value);

        SourceRecord transformedRecord = xform.apply(record);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof Struct);

        Struct transformedValue = (Struct) transformedRecord.value();
        System.out.println(transformedValue.get("primaryKey"));
        assertEquals(transformedValue.get("primaryKey"), "123-John_Doe-30");
        assertEquals(transformedValue.get("valueField"), "someValue");
        System.out.println("[Test] PK: " + transformedValue.get("primaryKey"));
        System.out.println("[Test] applyWithComplexSchemaTest Done");
    }

    @Test
    public void applyWithNonStructKeySchemaTest() {
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));

        Schema keySchema = Schema.INT32_SCHEMA;

        SourceRecord record = new SourceRecord(
                null, null, "test-topic", keySchema, 123, null, null);

        xform.apply(record);
    }

    @Test
    public void applyWithEmptyRecordTest() {
        xform.configure(Collections.singletonMap("field.name", "primaryKey"));

        Schema keySchema = SchemaBuilder.struct().build();
        Struct key = new Struct(keySchema);

        Schema valueSchema = SchemaBuilder.struct().build();
        Struct value = new Struct(valueSchema);

        // Use an empty record
        SourceRecord record = new SourceRecord(
                null, null, "test-topic", keySchema, key, valueSchema, value);

        SourceRecord transformedRecord = xform.apply(record);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof Struct);
        Struct transformedValue = (Struct) transformedRecord.value();
        assertFalse(transformedValue.schema().fields().isEmpty());
    }
}
