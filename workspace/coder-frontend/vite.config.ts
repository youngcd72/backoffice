import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 3000,
    proxy: {
      // coder-batch 관리 API (배치관리 SYLK050) - '/api' 보다 먼저 매칭되도록 위에 둔다
      '/api/v1/batch-jobs': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/batch-executions': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
