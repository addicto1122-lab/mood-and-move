import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";
import { fileURLToPath } from "node:url";

export default defineConfig(({ mode }) => {
  const envDir = fileURLToPath(new URL("../", import.meta.url));

  const env = loadEnv(mode, envDir, "");

  return {
    plugins: [react()],

    server: {
      proxy: {
        "/api": {
          target: env.API_URL,
          changeOrigin: true
        }
      }
    }
  };
});
