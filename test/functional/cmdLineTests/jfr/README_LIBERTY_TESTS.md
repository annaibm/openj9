# Liberty JFR Tests

## Overview

This directory contains JFR (Java Flight Recorder) tests that run against WebSphere Liberty to validate JFR functionality in a real application server environment.

## Test Structure

### Files Added

1. **jfr_liberty_tests.xml** - Test definition file containing Liberty-specific JFR test scenarios
2. **src/org/openj9/test/LibertyWorkload.java** - HTTP client that generates load against Liberty server
3. **playlist.xml** - Updated to include `cmdLineTester_jfrLiberty` test case

### Test Flow

The Liberty JFR tests follow this sequence:

1. **Create Liberty Server** - Creates a new Liberty test server instance
2. **Start Liberty with JFR** - Starts the server with `-XX:StartFlightRecording` JVM option
3. **Execute Workload** - Runs HTTP requests against the Liberty server to generate activity
4. **Stop Liberty Server** - Gracefully stops the server
5. **Verify JFR Recording** - Validates the JFR recording file was created and contains expected events
6. **Cleanup** - Removes the test server

## Prerequisites

### Liberty Runtime

The tests require WebSphere Liberty runtime to be available. Set the `LIBERTY_HOME` environment variable:

```bash
export LIBERTY_HOME=/path/to/wlp
```

### Supported Platforms

Currently configured for:
- Linux x86/x86_64 architectures
- JDK 11 and above

## Running the Tests

### Via Test Framework

The Liberty JFR tests are integrated into the existing test framework:

```bash
# Run all JFR tests (including Liberty)
make test_jfr

# Run only Liberty JFR tests
make test TEST=cmdLineTester_jfrLiberty
```

### Test Level

The Liberty tests are marked as **extended** level (not sanity) to avoid impacting quick test runs:

```xml
<levels>
    <level>extended</level>
</levels>
```

### Manual Execution

For manual testing or debugging:

```bash
# Set environment variables
export LIBERTY_HOME=/path/to/wlp
export TEST_JDK_HOME=/path/to/jdk
export TEST_RESROOT=/path/to/test/resources

# Run the test
java -jar cmdlinetester.jar \
  -DEXE="java" \
  -DJFR_EXE="$TEST_JDK_HOME/bin/jfr" \
  -DLIBERTY_HOME="$LIBERTY_HOME" \
  -DRESJAR="$TEST_RESROOT/jfr.jar" \
  -config jfr_liberty_tests.xml
```

## Test Scenarios

### 1. Basic Liberty Startup with JFR

Validates that Liberty can start successfully with JFR recording enabled.

**Expected Events:**
- `jdk.ExecutionSample`
- `jdk.CPULoad`
- `jdk.JavaThreadStatistics`

### 2. HTTP Request Handling

Generates HTTP traffic to exercise Liberty's request processing with JFR active.

**Workload Parameters:**
- Default: 100 HTTP requests
- Concurrent threads: 5
- Request delay: 100ms

### 3. Thread Events

Verifies JFR captures Liberty's thread creation and lifecycle events.

**Expected Events:**
- `jdk.ThreadStart`
- Thread names and IDs

### 4. GC Events

Validates garbage collection events are recorded during Liberty operation.

**Expected Events:**
- `jdk.GCHeapConfiguration`
- `jdk.YoungGenerationConfiguration`

## Customization

### Workload Configuration

Modify the workload in `jfr_liberty_tests.xml`:

```xml
<test id="Execute workload against Liberty server">
    <command>$EXE$ -cp $RESJAR$ org.openj9.test.LibertyWorkload http://localhost:9080 [requests] [threads]</command>
</test>
```

Parameters:
- `requests` - Total number of HTTP requests (default: 100)
- `threads` - Number of concurrent threads (default: 5)

### Liberty Configuration

To use a custom Liberty configuration:

1. Create a Liberty server with your configuration
2. Modify the test to use your server name instead of `jfrTestServer`

### JFR Options

Add custom JFR options in the server start command:

```xml
<command>$LIBERTY_HOME$/bin/server start jfrTestServer --jvm-options="-XX:StartFlightRecording=duration=60s,filename=custom.jfr"</command>
```

## Troubleshooting

### Liberty Not Found

**Error:** `Server command not found`

**Solution:** Ensure `LIBERTY_HOME` is set correctly:
```bash
ls $LIBERTY_HOME/bin/server  # Should exist
```

### Connection Refused

**Error:** `Connection refused` during workload execution

**Possible Causes:**
1. Liberty didn't start successfully - check Liberty logs
2. Port 9080 is already in use - change port in server.xml
3. Firewall blocking connections

**Check Liberty Logs:**
```bash
cat $LIBERTY_HOME/usr/servers/jfrTestServer/logs/messages.log
```

### JFR Recording Not Found

**Error:** `jfr print: could not read recording`

**Possible Causes:**
1. JFR not enabled - verify `-XX:StartFlightRecording` in JVM options
2. Server crashed before recording was written
3. Incorrect file path

**Verify Recording Location:**
```bash
ls $LIBERTY_HOME/usr/servers/jfrTestServer/defaultJ9recording.jfr
```

### Test Timeout

**Error:** Test exceeds 6000 second timeout

**Solution:** Reduce workload or increase timeout in `jfr_liberty_tests.xml`:
```xml
<suite id="Liberty JFR Tests" timeout="8000">
```

## Performance Considerations

### Test Duration

Approximate execution times:
- Liberty startup: 5-10 seconds
- Workload execution: 100-120 seconds (100 requests)
- Liberty shutdown: 2-5 seconds
- JFR validation: 30-40 seconds per event type
- **Total: ~200-250 seconds**

### Resource Usage

- **Memory:** Liberty requires ~256MB minimum heap
- **Disk:** JFR recording typically 5-20MB depending on duration
- **CPU:** Minimal impact, mostly I/O bound

### Optimization Tips

1. **Use Liberty Kernel:** Minimal runtime profile for faster startup
2. **Reduce Workload:** Fewer requests for smoke testing
3. **Parallel Validation:** Run JFR event checks concurrently
4. **Pre-staged Liberty:** Cache Liberty binaries to avoid download overhead

## Integration with CI/CD

### Jenkins Pipeline Example

```groovy
stage('Liberty JFR Tests') {
    when {
        expression { params.RUN_EXTENDED_TESTS }
    }
    steps {
        sh '''
            export LIBERTY_HOME=/opt/liberty/wlp
            make test TEST=cmdLineTester_jfrLiberty
        '''
    }
}
```

### Conditional Execution

The tests only run when:
1. JDK version is 11 or higher
2. Platform is Linux x86/x86_64
3. `LIBERTY_HOME` environment variable is set
4. Extended test level is requested

## Future Enhancements

Potential additions to the Liberty JFR test suite:

1. **Multiple Server Instances** - Test JFR with clustered Liberty
2. **Application Deployment** - Deploy WAR/EAR and test application-specific events
3. **Liberty-Specific Events** - Validate Liberty's custom JFR events (if any)
4. **Long-Running Tests** - Extended duration tests for memory leak detection
5. **Stress Testing** - High-load scenarios to test JFR overhead
6. **Different Liberty Profiles** - Test with various Liberty feature sets

## References

- [OpenJ9 JFR Documentation](https://www.eclipse.org/openj9/docs/jfr/)
- [WebSphere Liberty Documentation](https://www.ibm.com/docs/en/was-liberty)
- [JDK Flight Recorder Guide](https://docs.oracle.com/en/java/javase/11/jfr/)

## Support

For issues or questions:
1. Check Liberty logs: `$LIBERTY_HOME/usr/servers/jfrTestServer/logs/`
2. Review JFR recording: `jfr print defaultJ9recording.jfr`
3. Enable verbose output: Add `-verbose` to cmdlinetester command
4. Report issues to OpenJ9 test team
