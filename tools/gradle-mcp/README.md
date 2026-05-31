# gradle-mcp

A tiny, zero-dependency [MCP](https://modelcontextprotocol.io) server that runs
**Gradle builds on your machine** and exposes them to the assistant as async
tools. It exists so build verification (the NeoForge decompile + compile, which
needs real RAM, multiple cores, and minutes of uninterrupted runtime) happens on
your hardware instead of an ephemeral sandbox — then the assistant can poll the
result and confirm `BUILD SUCCESSFUL`.

No npm install. No external dependencies. Just Node.

## Prerequisites

- **Node.js 18+** on PATH (`node --version`).
- **JDK 25** installed (already done for this project). Gradle's toolchain will
  find it; if the launching app doesn't already expose it, set `JAVA_HOME` in the
  config `env` block below.
- A working Gradle wrapper in the project (`gradlew` / `gradlew.bat`).

## Tools

| Tool | What it does |
|------|--------------|
| `gradle_build` | Start a build async (default task `build`). Returns a `build_id` immediately. |
| `gradle_run_data` | Convenience for the `runData` datagen task. |
| `gradle_status` | Poll a build: `status`, `outcome` (SUCCESSFUL/FAILED once known), exit code, elapsed time, log tail. |
| `gradle_log` | Fetch the log; optional `grep` regex and `tail_lines`. |
| `gradle_stop` | Terminate a running build. |
| `gradle_list` | List builds started this session. |
| `gradle_clear_logs` | Delete this session's log files. |

The typical loop: `gradle_build` → `gradle_status` (repeat until `status` is
`succeeded`/`failed`) → `gradle_log` with a `grep` like `error|FAIL` if it failed.

## Setup — Claude desktop app (Cowork)

1. Open the MCP config file (create it if missing):

   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

2. Add a `gradle` server entry (merge into existing `mcpServers` if present):

   ```json
   {
     "mcpServers": {
       "gradle": {
         "command": "node",
         "args": [
           "C:\\Users\\dario\\Documents\\projects\\github\\nerospace\\tools\\gradle-mcp\\server.js"
         ],
         "env": {
           "GRADLE_PROJECT_DIR": "C:\\Users\\dario\\Documents\\projects\\github\\nerospace",
           "JAVA_HOME": "C:\\Program Files\\Eclipse Adoptium\\jdk-25"
         }
       }
     }
   }
   ```

   - Use **double backslashes** in JSON paths on Windows.
   - `JAVA_HOME` is optional — include it if Gradle can't otherwise find JDK 25.
     Point it at wherever your JDK 25 lives.

3. **Fully quit and reopen** the desktop app so it launches the server. The
   `gradle_*` tools then appear and the assistant can run builds for you.

## Setup — Claude Code CLI (alternative)

A project-scoped `.mcp.json` is included at the repo root pattern, or register it
directly:

```bash
claude mcp add gradle -- node ./tools/gradle-mcp/server.js
```

Set `GRADLE_PROJECT_DIR`/`JAVA_HOME` in your shell or in `.mcp.json`'s `env`.

## Quick local check (optional)

You can confirm the wrapper resolves without the app:

```bash
cd tools/gradle-mcp
GRADLE_PROJECT_DIR="<repo>" node server.js
# then paste a line and press enter:
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{}}}
# you should get an initialize result back. Ctrl-C to exit.
```

## Privacy / logging (POPIA & GDPR)

- Build logs are written **only to the local OS temp directory** and contain
  **only Gradle build diagnostics** (task names, compiler output, stack traces).
- Logs are **never transmitted** anywhere by this server except back through the
  MCP stdio channel you control — i.e. to the assistant you're already talking to.
- The server **does not log** environment variables, secrets, or personal data.
- Build output can incidentally include local file paths (which contain your
  username). Use `gradle_clear_logs` to wipe session logs, or delete
  `gradle-mcp-*.log` from your temp folder, whenever you want.
- Nothing is persisted beyond the temp log files; no telemetry, no network calls.

## Notes & limits

- Builds are tracked **in memory** for the server's lifetime; restarting the app
  starts a fresh session (the Gradle build cache on disk is untouched and still
  speeds up subsequent runs).
- The server only ever invokes the project's `gradlew` wrapper — it does not run
  arbitrary shell commands.
- First build does the one-time Minecraft decompile and will take several
  minutes; that's expected. Subsequent builds are fast thanks to Gradle caching.
