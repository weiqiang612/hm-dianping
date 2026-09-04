const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const APP_PORT = 8081;

// 1. Check if dev server is running
try {
  let isRunning = false;
  let pid = '';
  if (process.platform === 'win32') {
    try {
      const output = execSync(`netstat -ano | findstr :${APP_PORT}`, { stdio: ['pipe', 'pipe', 'ignore'] }).toString();
      const lines = output.split('\n');
      for (const line of lines) {
        if (line.includes('LISTENING')) {
          const parts = line.trim().split(/\s+/);
          pid = parts[parts.length - 1];
          isRunning = true;
          break;
        }
      }
    } catch (e) {}
  } else {
    try {
      pid = execSync(`lsof -t -i:${APP_PORT}`, { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim();
      if (pid) isRunning = true;
    } catch (e) {}
  }

  if (isRunning) {
    console.log(`✓ Dev server running on port ${APP_PORT} (PID ${pid})`);
  } else {
    console.log(`⚠️  Dev server is NOT running on port ${APP_PORT}.`);
    console.log(`   Run: bash init.sh (or appropriate startup command)`);
  }
} catch (err) {}

// 2. Git context
try {
  const branch = execSync('git branch --show-current', { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim();
  console.log('\n-- Branch & status --------------------------------');
  console.log(`Branch: ${branch || 'unknown'}`);
  const status = execSync('git status --short', { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim();
  if (status) {
    console.log(status.split('\n').slice(0, 10).join('\n'));
  }
} catch (err) {}

// 3. Recent commits
try {
  console.log('\n-- Recent commits ----------------------------------');
  const commits = execSync('git log -n 3 --oneline', { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim();
  console.log(commits);
} catch (err) {}

// 4. Current plan snapshot
try {
  console.log('\n-- Active task ------------------------------------');
  const planPath = path.join(process.cwd(), 'docs', '3-tasks', 'CURRENT_PLAN.md');
  if (fs.existsSync(planPath)) {
    const content = fs.readFileSync(planPath, 'utf8');
    const lines = content.split('\n');
    let found = false;
    let printedLines = 0;
    for (const line of lines) {
      if (line.startsWith('## Active feature')) {
        found = true;
      }
      if (found) {
        console.log(line);
        printedLines++;
        if (printedLines > 10) break;
      }
    }
  } else {
    console.log('No CURRENT_PLAN.md found - run /new-task to create your first task.');
  }
} catch (err) {}
console.log('');
