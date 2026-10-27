// One bridge tool call over stdio JSON-RPC: node tmp/w4/mcp.js <tool> '<json arguments>'
// (the bridge answers piped calls out of order, so one call per process).
const { spawn } = require("child_process");
const [tool, json] = process.argv.slice(2);
const args = json ? JSON.parse(json) : {};
const child = spawn("cmd", ["/c", "npx", "-y", "minecraft-mod-mcp", "mcp"], { stdio: ["pipe", "pipe", "inherit"] });
let buffer = "";
const timer = setTimeout(() => { console.error("timeout"); child.kill(); process.exit(2); }, 120000);
child.stdout.on("data", chunk => {
  buffer += chunk;
  let i;
  while ((i = buffer.indexOf("\n")) >= 0) {
    const line = buffer.slice(0, i).trim();
    buffer = buffer.slice(i + 1);
    if (!line) continue;
    let message;
    try { message = JSON.parse(line); } catch { continue; }
    if (message.id === 2) {
      const content = message.result ? message.result.content : null;
      if (content) for (const c of content) console.log(c.type === "text" ? c.text : "[" + c.type + "]");
      else console.log(JSON.stringify(message.error || message));
      clearTimeout(timer);
      child.stdin.end();
      child.kill();
      process.exit(message.error || (message.result && message.result.isError) ? 1 : 0);
    }
  }
});
const send = m => child.stdin.write(JSON.stringify(m) + "\n");
send({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2024-11-05", capabilities: {}, clientInfo: { name: "w6-check", version: "1" } } });
send({ jsonrpc: "2.0", method: "notifications/initialized" });
send({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: tool, arguments: args } });
