const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');

function buildApiUrl(path) {
  if (!API_BASE_URL) {
    throw new Error(
      `Missing VITE_API_BASE_URL. Attempted to call: ${path}. Set VITE_API_BASE_URL=https://teenyurl-lena.onrender.com and restart Vite.`,
    );
  }

  return `${API_BASE_URL}${path}`;
}

async function parseJsonResponse(response, url) {
  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    const message =
      payload?.message ||
      payload?.error ||
      `Request failed with status ${response.status}`;
    throw new Error(`${message}. URL called: ${url}`);
  }

  return payload;
}

async function fetchJson(url, options) {
  try {
    const response = await fetch(url, options);
    return parseJsonResponse(response, url);
  } catch (error) {
    if (error.message.includes('URL called:')) {
      throw error;
    }

    throw new Error(`${error.message}. URL called: ${url}`);
  }
}

export async function createShortUrl({ originalUrl, alias }) {
  const url = buildApiUrl('/api/urls');
  const requestBody = {
    originalUrl,
    ...(alias ? { customAlias: alias } : {}),
  };

  return fetchJson(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(requestBody),
  });
}

export async function getUrlStats(shortCode) {
  const url = buildApiUrl(`/api/urls/${shortCode}/stats`);
  return fetchJson(url);
}
