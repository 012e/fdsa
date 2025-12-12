#!/usr/bin/env fish

# Integration Test Runner
# Runs the integration tests for snippet ingestion workflow

set -l SCRIPT_DIR (dirname (status -f))

echo "🧪 Running Integration Tests for Snippet Ingestion Workflow"
echo "============================================================"
echo ""

# Check if .env file exists
if not test -f "$SCRIPT_DIR/.env"
    echo "❌ Error: .env file not found"
    echo "Please create a .env file with your OPENAI_API_KEY"
    exit 1
end

# Check if OPENAI_API_KEY is set
set -l api_key (grep OPENAI_API_KEY "$SCRIPT_DIR/.env" | cut -d '=' -f2)
if test -z "$api_key"
    echo "❌ Error: OPENAI_API_KEY not found in .env"
    exit 1
end

echo "✅ Found OPENAI_API_KEY in .env"
echo ""

# Check if Docker is running
if not docker info > /dev/null 2>&1
    echo "❌ Error: Docker is not running"
    echo "Please start Docker and try again"
    exit 1
end

echo "✅ Docker is running"
echo ""

# Run the tests
cd "$SCRIPT_DIR"
echo "🚀 Starting integration tests..."
echo ""

pytest -v -s $argv

set -l exit_code $status

if test $exit_code -eq 0
    echo ""
    echo "============================================================"
    echo "✅ All tests passed!"
    echo "============================================================"
else
    echo ""
    echo "============================================================"
    echo "❌ Tests failed with exit code: $exit_code"
    echo "============================================================"
end

exit $exit_code
