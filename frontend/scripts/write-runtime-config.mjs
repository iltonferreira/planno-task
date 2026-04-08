import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const trimTrailingSlashes = (value) => value.replace(/\/+$/, '');

const normalizeBaseUrl = (value) => {
  const trimmed = trimTrailingSlashes((value ?? '').trim());

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

const runtimeConfig = {
  apiBaseUrl: normalizeBaseUrl(process.env.PLANNO_API_BASE_URL ?? '')
};

const outputDirectory = join(process.cwd(), 'public');
const outputFile = join(outputDirectory, 'runtime-config.js');
const fileContents = `window.__PLANNO_CONFIG__ = ${JSON.stringify(runtimeConfig, null, 2)};\n`;

mkdirSync(outputDirectory, { recursive: true });
writeFileSync(outputFile, fileContents, 'utf8');

console.log(
  `[planno] runtime-config.js atualizado com PLANNO_API_BASE_URL=${runtimeConfig.apiBaseUrl || '(same-origin)'}`
);
