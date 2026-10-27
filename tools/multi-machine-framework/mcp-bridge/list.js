// Lists the bridge's tool names: node tmp/w7/list.js
const { spawn } = require("child_process");
const child = spawn("cmd", ["/c", "npx", "-y", "minecraft-mod-mcp", "mcp"], { stdio: ["pipe", "pipe", "inherit"] });
let buffer = "";
const timer = setTimeout(() => { console.error("timeout"); child.kill(); process.exit(2); }, 60000);
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
      for (const t of message.result.tools) console.log(t.name + ": " + (t.description || "").slice(0, 90).replace(/\s+/g, " "));
      clearTimeout(timer);
      child.stdin.end();
      child.kill();
      process.exit(0);
    }
  }
});
const send = m => child.stdin.write(JSON.stringify(m) + "\n");
send({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2024-11-05", capabilities: {}, clientInfo: { name: "w7-list", version: "1" } } });
send({ jsonrpc: "2.0", method: "notifications/initialized" });
send({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });
