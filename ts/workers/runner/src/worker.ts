import { NativeConnection, Worker } from '@temporalio/worker';
import * as activities from './activities/index.js';
import { getConfig } from '@fdsa/shared';
import { fileURLToPath } from 'url';
import path from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function run() {
  try {
    const config = getConfig();
    
    console.log('Starting Temporal worker...');
    console.log(`Connecting to Temporal at ${config.temporal.host}`);
    console.log(`Task queue: ${config.temporal.taskQueue}`);

    // Create connection to Temporal server
    const connection = await NativeConnection.connect({
      address: config.temporal.host,
    });

    // Create worker
    const worker = await Worker.create({
      connection,
      namespace: config.temporal.namespace,
      taskQueue: config.temporal.taskQueue,
      workflowsPath: path.join(__dirname, 'workflows'),
      activities,
    });

    console.log('Worker created successfully');
    console.log('Available activities:', Object.keys(activities).join(', '));
    console.log('Workflows path:', path.join(__dirname, 'workflows'));
    console.log('\nWorker is running and ready to process workflows...\n');

    // Run the worker
    await worker.run();
  } catch (error) {
    console.error('Fatal error in worker:', error);
    process.exit(1);
  }
}

run().catch((err) => {
  console.error('Unhandled error:', err);
  process.exit(1);
});
