// Opt-in local capacity profile; never point this test at a public host.
import http from 'k6/http';
import { check, sleep } from 'k6';

const base = __ENV.LOAD_BASE_URL || 'http://host.docker.internal:8080';
if (!/^http:\/\/(?:host\.docker\.internal|localhost|127\.0\.0\.1)(?::\d{1,5})?$/.test(base)) {
  throw new Error('Capacity profile is restricted to an isolated local HTTP target');
}
if (!__ENV.SMOKE_USERNAME || !__ENV.SMOKE_PASSWORD) {
  throw new Error('Disposable gate credentials are required');
}
const loginFixtures = ['ADMIN', 'TEACHER', 'COORDINATOR', 'COORDINATOR2'].map(role => {
  const username = __ENV[`${role}_USERNAME`];
  const password = __ENV[`${role}_PASSWORD`];
  if (!username || !password) throw new Error(`Disposable ${role} load fixture is required`);
  return { username, password };
});

export const options = {
  scenarios: {
    authenticated_reads: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 10 },
        { duration: '15s', target: 25 },
        { duration: '10s', target: 50 },
        { duration: '5s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
    concurrent_sign_ins: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '0s',
      stages: [
        { duration: '15s', target: 3 },
        { duration: '15s', target: 5 },
        { duration: '10s', target: 10 },
        { duration: '5s', target: 0 },
      ],
      gracefulRampDown: '5s',
      exec: 'signInStress',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
    checks: ['rate>0.99'],
  },
};

function csrfHeaders() {
  const response = http.get(`${base}/api/v1/auth/csrf`, { timeout: '5s' });
  if (response.status !== 200) throw new Error(`CSRF bootstrap failed: HTTP ${response.status}`);
  const token = response.json('token');
  if (response.json('headerName') !== 'X-XSRF-TOKEN' || typeof token !== 'string') {
    throw new Error('Invalid CSRF bootstrap');
  }
  // k6 keeps the bootstrap cookie in its per-VU cookie jar.
  return { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token };
}

export function setup() {
  const headers = csrfHeaders();
  const response = http.post(`${base}/api/v1/auth/sign-in`, JSON.stringify({
    username: __ENV.SMOKE_USERNAME,
    password: __ENV.SMOKE_PASSWORD,
  }), { headers, timeout: '5s' });
  if (response.status !== 200 || !response.cookies.JWT_TOKEN?.[0]?.value) {
    throw new Error(`Isolated login fixture failed: HTTP ${response.status}`);
  }
  return { cookie: `JWT_TOKEN=${response.cookies.JWT_TOKEN[0].value}` };
}

export default function (fixture) {
  const routes = ['/api/v1/auth/me', '/api/v1/notifications/unread'];
  const path = routes[__ITER % routes.length];
  const response = http.get(`${base}${path}`, {
    headers: { Cookie: fixture.cookie },
    tags: { endpoint: path },
    timeout: '5s',
  });
  check(response, { 'authenticated endpoint returns 200': r => r.status === 200 });
  sleep(0.2);
}

export function signInStress() {
  const identity = loginFixtures[(__VU - 1) % loginFixtures.length];
  const headers = csrfHeaders();
  const response = http.post(`${base}/api/v1/auth/sign-in`, JSON.stringify(identity), {
    headers,
    tags: { workload: 'auth', endpoint: '/api/v1/auth/sign-in' },
    timeout: '5s',
  });
  check(response, {
    'concurrent sign-in returns 200': r => r.status === 200,
    'sign-in issues an HttpOnly JWT cookie': r =>
      r.cookies.JWT_TOKEN?.[0]?.http_only === true,
  });
  sleep(1);
}
