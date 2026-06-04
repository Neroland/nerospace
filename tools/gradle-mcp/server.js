#!/usr/bin/env node
/*
 * gradle-mcp: a tiny, zero-dependency MCP server that runs Gradle builds
 * on the local machine and exposes them as async tools.
 *
 * Why this exists: build verification (NeoForge decompile + compile) needs
 * real RAM, multiple cores, and minutes of uninterrupted runtime that a
 * sandbox cannot provide. This server runs `gradlew` on YOUR machine and
 * lets the assistant start a build and poll its progress.
 *
 * Transport: MCP stdio (newline-delimited JSON-RPC 2.0).
 *   - stdout: ONLY JSON-RPC messages (one per line).
 *   - stderr: human-readable diagnostics.
 *
 * Privacy (POPIA/GDPR): build logs are written only to the local OS temp
 * directory, contain only Gradle build diagnostics, are never transmitted
 * anywhere except back through the MCP channel you control, and can be
 * wiped with the `gradle_clear_logs` tool. No environment variables,
 * secrets, or personal data are logged by this server itself.
 */

'use strict';

const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');

const SERVER_NAME = 'gradle-mcp';
const SERVER_VERSION = '1.1.0';
const DEFAULT_PROTOCOL = '2025-06-18';

// Where to run Gradle. Override per-call with the `project_dir` argument.
const DEFAULT_PROJECT_DIR =
  process.env.GRADLE_PROJECT_DIR || process.cwd();

// In-memory registry of builds for this server lifetime.
// id -> { id, tasks, args, projectDir, logPath, status, exitCode, startedAt, endedAt, child }
const builds = new Map();
let lastBuildId = null;

function log(...a) {
  process.stderr.write(`[${SERVER_NAME}] ${a.join(' ')}\n`);
}

function gradlewCommand(projectDir) {
  if (process.platform === 'win32') {
    return { cmd: path.join(projectDir, 'gradlew.bat'), useShell: true };
  }
  return { cmd: path.join(projectDir, 'gradlew'), useShell: false };
}

function tailFile(p, lines) {
  try {
    const data = fs.readFileSync(p, 'utf8');
    if (!lines || lines <= 0) return data;
    const arr = data.split(/\r?\n/);
    return arr.slice(Math.max(0, arr.length - lines)).join('\n');
  } catch {
    return '';
  }
}

function detectOutcome(p) {
  const txt = tailFile(p, 0);
  if (/BUILD SUCCESSFUL/.test(txt)) return 'SUCCESSFUL';
  if (/BUILD FAILED/.test(txt)) return 'FAILED';
  return null;
}

function startBuild({ tasks, extra_args, project_dir }) {
  const projectDir = project_dir || DEFAULT_PROJECT_DIR;
  const wrapper = gradlewCommand(projectDir);

  if (!fs.existsSync(projectDir)) {
    throw new Error(`project_dir does not exist: ${projectDir}`);
  }
  if (!fs.existsSync(wrapper.cmd)) {
    throw new Error(
      `Gradle wrapper not found at ${wrapper.cmd}. ` +
        `Set GRADLE_PROJECT_DIR or pass project_dir.`
    );
  }

  const id = crypto.randomUUID().slice(0, 8);
  const logPath = path.join(os.tmpdir(), `gradle-mcp-${id}.log`);
  const taskList = Array.isArray(tasks) && tasks.length ? tasks : ['build'];
  const argList = Array.isArray(extra_args) ? extra_args : [];
  // --console=plain keeps the log clean and parseable.
  const fullArgs = [...taskList, ...argList, '--console=plain'];

  const out = fs.openSync(logPath, 'w');
  fs.writeSync(
    out,
    `# gradle-mcp build ${id}\n# dir: ${projectDir}\n# cmd: gradlew ${fullArgs.join(' ')}\n# started: ${new Date().toISOString()}\n\n`
  );

  const child = spawn(wrapper.cmd, fullArgs, {
    cwd: projectDir,
    shell: wrapper.useShell,
    env: process.env, // inherits JAVA_HOME / PATH from the launching app
    stdio: ['ignore', out, out],
    windowsHide: true,
  });

  const rec = {
    id,
    tasks: taskList,
    args: argList,
    projectDir,
    logPath,
    status: 'running',
    exitCode: null,
    startedAt: Date.now(),
    endedAt: null,
    child,
  };
  builds.set(id, rec);
  lastBuildId = id;

  child.on('error', (err) => {
    rec.status = 'error';
    rec.endedAt = Date.now();
    try {
      fs.appendFileSync(logPath, `\n[gradle-mcp] spawn error: ${err.message}\n`);
    } catch {}
    log(`build ${id} spawn error: ${err.message}`);
  });

  child.on('exit', (code, signal) => {
    rec.exitCode = code;
    rec.endedAt = Date.now();
    rec.status =
      signal === 'SIGTERM' || signal === 'SIGKILL'
        ? 'stopped'
        : code === 0
        ? 'succeeded'
        : 'failed';
    try {
      fs.closeSync(out);
    } catch {}
    log(`build ${id} ${rec.status} (code=${code}, signal=${signal})`);
  });

  return rec;
}

function resolveBuild(build_id) {
  const id = build_id || lastBuildId;
  if (!id) return null;
  return builds.get(id) || null;
}

function summarize(rec, tailLines = 0) {
  const elapsedMs = (rec.endedAt || Date.now()) - rec.startedAt;
  const outcome = detectOutcome(rec.logPath);
  const obj = {
    build_id: rec.id,
    status: rec.status, // running | succeeded | failed | stopped | error
    outcome, // SUCCESSFUL | FAILED | null (still running / unknown)
    exit_code: rec.exitCode,
    project_dir: rec.projectDir,
    command: `gradlew ${[...rec.tasks, ...rec.args].join(' ')}`,
    elapsed_seconds: Math.round(elapsedMs / 1000),
    log_file: rec.logPath,
  };
  if (tailLines > 0) obj.log_tail = tailFile(rec.logPath, tailLines);
  return obj;
}

// ---- Tool definitions -----------------------------------------------------

const TOOLS = [
  {
    name: 'gradle_build',
    description:
      'Start a Gradle build asynchronously and return immediately with a build_id. ' +
      'Use gradle_status to poll. Defaults to the `build` task. Runs `gradlew` ' +
      'in the configured project directory on this machine.',
    inputSchema: {
      type: 'object',
      properties: {
        tasks: {
          type: 'array',
          items: { type: 'string' },
          description: 'Gradle tasks to run (default ["build"]).',
        },
        extra_args: {
          type: 'array',
          items: { type: 'string' },
          description: 'Additional CLI args, e.g. ["--stacktrace","--info"].',
        },
        project_dir: {
          type: 'string',
          description:
            'Override the project directory (defaults to GRADLE_PROJECT_DIR).',
        },
      },
    },
  },
  {
    name: 'gradle_run_data',
    description:
      'Convenience: start the `runData` datagen task asynchronously (equivalent ' +
      'to gradle_build with tasks ["runData"]). Returns a build_id to poll.',
    inputSchema: {
      type: 'object',
      properties: {
        extra_args: { type: 'array', items: { type: 'string' } },
        project_dir: { type: 'string' },
      },
    },
  },
  {
    name: 'gradle_analyze',
    description:
      'Convenience: start the `ecjCheck` task asynchronously — runs the Eclipse ' +
      'compiler (the same analyzer as the VS Code Problems panel, configured by ' +
      'tools/ecj.prefs) over the main sources. Poll with gradle_status, then read ' +
      'diagnostics with gradle_log (grep "WARNING|ERROR" for a summary).',
    inputSchema: {
      type: 'object',
      properties: {
        extra_args: { type: 'array', items: { type: 'string' } },
        project_dir: { type: 'string' },
      },
    },
  },
  {
    name: 'gradle_status',
    description:
      'Poll a build. Returns status, outcome (BUILD SUCCESSFUL/FAILED once known), ' +
      'exit code, elapsed time, and a tail of the log. Omit build_id for the latest build.',
    inputSchema: {
      type: 'object',
      properties: {
        build_id: { type: 'string' },
        tail_lines: {
          type: 'number',
          description: 'How many trailing log lines to include (default 40).',
        },
      },
    },
  },
  {
    name: 'gradle_log',
    description:
      'Fetch build log output. Optionally filter to lines matching a regex (grep) ' +
      'and/or limit to the last N lines. Omit build_id for the latest build.',
    inputSchema: {
      type: 'object',
      properties: {
        build_id: { type: 'string' },
        tail_lines: { type: 'number' },
        grep: {
          type: 'string',
          description: 'Case-insensitive regex; only matching lines are returned.',
        },
      },
    },
  },
  {
    name: 'gradle_stop',
    description: 'Stop a running build (sends terminate). Omit build_id for the latest build.',
    inputSchema: {
      type: 'object',
      properties: { build_id: { type: 'string' } },
    },
  },
  {
    name: 'gradle_list',
    description: 'List all builds started during this server session.',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'gradle_clear_logs',
    description:
      'Delete all gradle-mcp log files created during this session (privacy/cleanup).',
    inputSchema: { type: 'object', properties: {} },
  },
];

// ---- Tool dispatch --------------------------------------------------------

function callTool(name, args) {
  args = args || {};
  switch (name) {
    case 'gradle_build': {
      const rec = startBuild({
        tasks: args.tasks,
        extra_args: args.extra_args,
        project_dir: args.project_dir,
      });
      return summarize(rec, 0);
    }
    case 'gradle_run_data': {
      const rec = startBuild({
        tasks: ['runData'],
        extra_args: args.extra_args,
        project_dir: args.project_dir,
      });
      return summarize(rec, 0);
    }
    case 'gradle_analyze': {
      const rec = startBuild({
        tasks: ['ecjCheck'],
        extra_args: args.extra_args,
        project_dir: args.project_dir,
      });
      return summarize(rec, 0);
    }
    case 'gradle_status': {
      const rec = resolveBuild(args.build_id);
      if (!rec) throw new Error('No build found. Start one with gradle_build.');
      const tail = typeof args.tail_lines === 'number' ? args.tail_lines : 40;
      return summarize(rec, tail);
    }
    case 'gradle_log': {
      const rec = resolveBuild(args.build_id);
      if (!rec) throw new Error('No build found. Start one with gradle_build.');
      let text = tailFile(rec.logPath, args.tail_lines || 0);
      if (args.grep) {
        const re = new RegExp(args.grep, 'i');
        text = text
          .split(/\r?\n/)
          .filter((l) => re.test(l))
          .join('\n');
      }
      return { build_id: rec.id, log: text };
    }
    case 'gradle_stop': {
      const rec = resolveBuild(args.build_id);
      if (!rec) throw new Error('No build found.');
      if (rec.status === 'running' && rec.child) {
        rec.child.kill('SIGTERM');
        return { build_id: rec.id, status: 'stopping' };
      }
      return { build_id: rec.id, status: rec.status, note: 'not running' };
    }
    case 'gradle_list': {
      return {
        builds: [...builds.values()].map((r) => summarize(r, 0)),
      };
    }
    case 'gradle_clear_logs': {
      let removed = 0;
      for (const r of builds.values()) {
        try {
          if (fs.existsSync(r.logPath)) {
            fs.unlinkSync(r.logPath);
            removed++;
          }
        } catch {}
      }
      return { removed_log_files: removed };
    }
    default:
      throw new Error(`Unknown tool: ${name}`);
  }
}

// ---- JSON-RPC / MCP plumbing ---------------------------------------------

function send(msg) {
  process.stdout.write(JSON.stringify(msg) + '\n');
}

function reply(id, result) {
  send({ jsonrpc: '2.0', id, result });
}

function replyError(id, code, message) {
  send({ jsonrpc: '2.0', id, error: { code, message } });
}

function handleMessage(msg) {
  // Notifications have no id and need no response.
  const isNotification = msg.id === undefined || msg.id === null;

  try {
    switch (msg.method) {
      case 'initialize': {
        const clientProto =
          msg.params && msg.params.protocolVersion
            ? msg.params.protocolVersion
            : DEFAULT_PROTOCOL;
        reply(msg.id, {
          protocolVersion: clientProto,
          capabilities: { tools: { listChanged: false } },
          serverInfo: { name: SERVER_NAME, version: SERVER_VERSION },
        });
        return;
      }
      case 'notifications/initialized':
      case 'initialized':
        return; // no response
      case 'ping':
        if (!isNotification) reply(msg.id, {});
        return;
      case 'tools/list':
        reply(msg.id, { tools: TOOLS });
        return;
      case 'tools/call': {
        const { name, arguments: args } = msg.params || {};
        try {
          const result = callTool(name, args);
          reply(msg.id, {
            content: [
              { type: 'text', text: JSON.stringify(result, null, 2) },
            ],
            isError: false,
          });
        } catch (err) {
          reply(msg.id, {
            content: [{ type: 'text', text: `Error: ${err.message}` }],
            isError: true,
          });
        }
        return;
      }
      default:
        if (!isNotification) {
          replyError(msg.id, -32601, `Method not found: ${msg.method}`);
        }
        return;
    }
  } catch (err) {
    if (!isNotification) replyError(msg.id, -32603, err.message);
  }
}

function main() {
  log(`starting (project dir: ${DEFAULT_PROJECT_DIR})`);
  let buffer = '';
  process.stdin.setEncoding('utf8');
  process.stdin.on('data', (chunk) => {
    buffer += chunk;
    let idx;
    while ((idx = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, idx).trim();
      buffer = buffer.slice(idx + 1);
      if (!line) continue;
      let msg;
      try {
        msg = JSON.parse(line);
      } catch (e) {
        log(`bad JSON: ${e.message}`);
        continue;
      }
      handleMessage(msg);
    }
  });
  process.stdin.on('end', () => process.exit(0));
}

main();
