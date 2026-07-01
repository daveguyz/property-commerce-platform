import { defineConfig } from 'vite';
import { resolve } from 'path';

/**
 * Builds the bundled SDK that ships to the CDN.
 * Three output formats from one entry point:
 *   - iife: <script src="pcp.min.js"> — sets window.PCP, zero build step needed
 *   - es:   import { ... } from '@property-commerce/sdk' — for bundled apps
 *   - cjs:  require() — for Node-based SSR/testing environments
 *
 * The entry (bundle-entry.js) imports core + all widget packages so a
 * single file is enough for a host site to mount any widget.
 */
export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'bundle-entry.js'),
      name: 'PCP',
      formats: ['iife', 'es', 'cjs'],
      fileName: (format) => {
        if (format === 'iife') return 'pcp.min.js';
        if (format === 'es') return 'pcp.esm.js';
        return 'pcp.cjs.js';
      },
    },
    rollupOptions: {
      output: {
        // IIFE build must be self-contained — no externalised deps
        // since it's loaded directly via <script src>.
      },
    },
    sourcemap: true,
    minify: 'esbuild',
  },
});
