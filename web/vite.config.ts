import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiProxyTarget = env.API_PROXY_TARGET || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
          secure: true,
        },
      },
      port: 5173,
    },
    define: {
      global: 'globalThis', // 이거 추가
    },
  };
});
