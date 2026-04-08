type RuntimeConfig = {
  apiBaseUrl: string;
};

type RuntimeWindow = typeof globalThis & {
  __PLANNO_CONFIG__?: Partial<RuntimeConfig>;
};

const trimTrailingSlashes = (value: string): string => value.replace(/\/+$/, '');

const normalizeBaseUrl = (value: string): string => {
  const trimmed = trimTrailingSlashes(value.trim());

  if (!trimmed) {
    return '';
  }

  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }

  const protocol = trimmed.startsWith('localhost') || trimmed.startsWith('127.0.0.1')
    ? 'http://'
    : 'https://';

  return `${protocol}${trimmed}`;
};

export const getRuntimeConfig = (): RuntimeConfig => {
  const runtimeWindow = globalThis as RuntimeWindow;

  return {
    apiBaseUrl: normalizeBaseUrl(runtimeWindow.__PLANNO_CONFIG__?.apiBaseUrl ?? '')
  };
};

export const resolveApiUrl = (url: string): string => {
  if (!url.startsWith('/api')) {
    return url;
  }

  const { apiBaseUrl } = getRuntimeConfig();
  return apiBaseUrl ? `${apiBaseUrl}${url}` : url;
};
