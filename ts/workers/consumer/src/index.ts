import { Consumer } from '@platformatic/kafka';
import { Client } from '@temporalio/client';
import {
  getConfig,
  SnippetCreatedEventSchema,
  SnippetUpdatedEventSchema,
  type SnippetCreatedEvent,
  type SnippetUpdatedEvent,
} from '@fdsa/shared';

/**
 * Start a snippet ingestion workflow via Temporal
 */
async function startWorkflow(
  temporalClient: Client,
  snippetId: string,
  code: string
): Promise<void> {
  try {
    const config = getConfig();
    const workflowId = `snippet-ingestion-${snippetId}`;

    await temporalClient.workflow.start('snippetIngestionWorkflow', {
      taskQueue: config.temporal.taskQueue,
      workflowId,
      args: [snippetId, code],
    });

    console.log(`Started workflow ${workflowId} for snippet ${snippetId}`);
  } catch (error) {
    console.error(`Error starting workflow for snippet ${snippetId}:`, error);
    throw error;
  }
}

/**
 * Main function to consume Kafka events and trigger workflows
 */
async function run() {
  try {
    const config = getConfig();

    console.log('Starting Kafka consumer...');
    console.log(`Connecting to Temporal at ${config.temporal.host}`);

    // Connect to Temporal
    const temporalClient = await Client.connect({
      address: config.temporal.host,
      namespace: config.temporal.namespace,
    });
    console.log('Connected to Temporal');

    console.log(`Connecting to Kafka at ${config.kafka.bootstrapServers}`);
    console.log(`Consumer group: ${config.kafka.groupId}`);

    // Create Kafka consumer
    const consumer = new Consumer({
      brokers: config.kafka.bootstrapServers.split(','),
      groupId: config.kafka.groupId,
      deserializers: {
        value: (buffer: Buffer) => JSON.parse(buffer.toString('utf-8')),
      },
    });

    // Join consumer group
    await consumer.joinGroup({
      protocols: [
        {
          name: 'roundrobin',
          version: 1,
        },
      ],
    });

    console.log('Joined consumer group');

    // Topics to subscribe to
    const topics = [
      config.kafka.topics.snippetCreated,
      config.kafka.topics.snippetUpdated,
    ];
    console.log(`Subscribing to topics: ${topics.join(', ')}`);

    // Start consuming messages
    const stream = consumer.consume({
      topics,
      mode: 'LATEST',
    });

    console.log('\nConsumer is running and waiting for events...\n');

    // Process messages
    for await (const message of stream) {
      try {
        const topic = message.topic;
        const value = message.value;

        console.log(`Received message from topic: ${topic}`);

        if (topic === config.kafka.topics.snippetCreated) {
          // Parse and validate snippet.created event
          const event: SnippetCreatedEvent = SnippetCreatedEventSchema.parse(value);
          console.log(`Processing snippet.created for snippet ${event.id}`);
          await startWorkflow(temporalClient, event.id, event.code);
        } else if (topic === config.kafka.topics.snippetUpdated) {
          // Parse and validate snippet.updated event
          const event: SnippetUpdatedEvent = SnippetUpdatedEventSchema.parse(value);
          console.log(`Processing snippet.updated for snippet ${event.id}`);
          await startWorkflow(temporalClient, event.id, event.code);
        } else {
          console.log(`Ignoring message from unknown topic: ${topic}`);
        }

        // Commit the message offset
        await message.commit();
      } catch (error) {
        console.error('Error processing message:', error);
        // Continue processing other messages even if one fails
      }
    }
  } catch (error) {
    console.error('Fatal error in consumer:', error);
    process.exit(1);
  }
}

run().catch((err) => {
  console.error('Unhandled error:', err);
  process.exit(1);
});
