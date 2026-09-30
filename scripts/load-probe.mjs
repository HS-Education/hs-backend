// Bounded local probe. Never targets an external or production host.
import { performance } from 'node:perf_hooks';

const base = new URL(process.env.LOAD_BASE_URL ?? 'http://127.0.0.1:8080');
if (!['localhost', '127.0.0.1', '[::1]'].includes(base.hostname) || base.protocol !== 'http:') {
  throw new Error('LOAD_BASE_URL must be a local HTTP address');
}
const username = process.env.SMOKE_USERNAME;
const password = process.env.SMOKE_PASSWORD;
if (!username || !password) throw new Error('SMOKE_USERNAME and SMOKE_PASSWORD are required');
const users = Number(process.env.LOAD_USERS ?? 10);
const requests = Number(process.env.LOAD_REQUESTS ?? 200);
const maxP95Ms = Number(process.env.LOAD_P95_MS ?? 2000);
const maxErrorRate = Number(process.env.LOAD_MAX_ERROR_RATE ?? 0.01);
if (!Number.isInteger(users) || users < 1 || users > 25 || !Number.isInteger(requests)
    || requests < users || requests > 1000 || maxP95Ms <= 0 || maxErrorRate < 0 || maxErrorRate > 1) {
  throw new Error('Load parameters out of bounded range');
}

const credentials = [];
for (let i = 0; i < users; i++) {
  const bootstrap = await fetch(new URL('/api/v1/auth/csrf', base), { signal: AbortSignal.timeout(5000) });
  if (!bootstrap.ok) throw new Error(`CSRF bootstrap failed: HTTP ${bootstrap.status}`);
  const csrf = await bootstrap.json();
  if (csrf.headerName !== 'X-XSRF-TOKEN' || typeof csrf.token !== 'string') throw new Error('Invalid CSRF bootstrap');
  const csrfCookie = bootstrap.headers.getSetCookie().map(line => line.split(';', 1)[0]).join('; ');
  const response = await fetch(new URL('/api/v1/auth/sign-in', base), {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'X-XSRF-TOKEN': csrf.token, cookie: csrfCookie },
    body: JSON.stringify({ username, password }),
    signal: AbortSignal.timeout(5000),
  });
  if (response.status !== 200) throw new Error(`Fixture sign-in failed: HTTP ${response.status}`);
  const cookie = response.headers.getSetCookie().map(line => line.split(';', 1)[0]).join('; ');
  if (!cookie.includes('JWT_TOKEN=')) throw new Error('Fixture did not receive JWT cookie');
  credentials.push(cookie);
}

const samples = [];
let errors = 0;
const started = performance.now();
const paths = ['/api/v1/auth/me', '/api/v1/notifications/unread'];
await Promise.all(credentials.map(async (cookie, worker) => {
  for (let index = worker; index < requests; index += users) {
    const begin = performance.now();
    try {
      const response = await fetch(new URL(paths[index % paths.length], base), {
        headers: { cookie },
        signal: AbortSignal.timeout(5000),
      });
      await response.arrayBuffer();
      if (response.status !== 200) errors++;
    } catch {
      errors++;
    } finally {
      samples.push(performance.now() - begin);
    }
  }
}));
const elapsedSeconds = (performance.now() - started) / 1000;
samples.sort((a, b) => a - b);
const p95Ms = samples[Math.ceil(samples.length * 0.95) - 1];
const result = {
  profile: 'local authenticated read endpoints', users, requests, errors,
  errorRate: +(errors / requests).toFixed(4), p95Ms: +p95Ms.toFixed(1),
  requestsPerSecond: +(requests / elapsedSeconds).toFixed(1),
  thresholds: { maxP95Ms, maxErrorRate },
  passed: p95Ms <= maxP95Ms && errors / requests <= maxErrorRate,
};
process.stdout.write(`${JSON.stringify(result)}\n`);
if (!result.passed) process.exitCode = 1;
