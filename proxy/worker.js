// Optional AI proxy for MindAtlas (Cloudflare Workers / any edge runtime with fetch).
// Keeps the OpenRouter key server-side so it never ships inside the APK.
// Deploy: `npx wrangler deploy`, then `npx wrangler secret put OPENROUTER_KEY`
//         and optionally `npx wrangler secret put APP_TOKEN` (shared token sent by the app as its "API key").
// In the app's Settings window set Base URL = https://<your-worker>/v1 and API Key = APP_TOKEN.
const UPSTREAM = "https://openrouter.ai/api/v1/chat/completions";
const ALLOWED_MODELS = ["deepseek/deepseek-chat-v3-0324", "google/gemini-2.5-flash"];

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (req.method !== "POST" || !url.pathname.endsWith("/chat/completions")) {
      return new Response("MindAtlas proxy OK", { status: url.pathname === "/" ? 200 : 404 });
    }
    if (env.APP_TOKEN && req.headers.get("Authorization") !== `Bearer ${env.APP_TOKEN}`) {
      return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401 });
    }
    let body;
    try { body = await req.json(); } catch { return new Response("bad json", { status: 400 }); }
    if (!ALLOWED_MODELS.includes(body.model)) body.model = ALLOWED_MODELS[0];
    body.max_tokens = Math.min(body.max_tokens || 800, 1500);
    const r = await fetch(UPSTREAM, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${env.OPENROUTER_KEY}`,
        "Content-Type": "application/json",
        "X-Title": "MindAtlas PICO",
      },
      body: JSON.stringify(body),
    });
    return new Response(r.body, { status: r.status, headers: { "Content-Type": "application/json" } });
  },
};
