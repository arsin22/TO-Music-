import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import {defineConfig} from 'vite';
import {VitePWA} from 'vite-plugin-pwa';

export default defineConfig(() => {
  return {
    plugins: [
      react(),
      tailwindcss(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['icon.svg', 'apple-touch-icon.png'],
        manifest: {
          id: '/',
          name: 'TO Music',
          short_name: 'TO Music',
          description:
            'موزیک‌پلیر وب‌اپ قابل نصب روی گوشی با تم شیشه‌ای تیره، انیمیشن‌های زنده، متن هماهنگ آهنگ و ابزار زنگ گوشی',
          theme_color: '#09090b',
          background_color: '#09090b',
          display: 'standalone',
          start_url: '/',
          scope: '/',
          icons: [
            {
              src: '/pwa-192x192.png',
              sizes: '192x192',
              type: 'image/png',
              purpose: 'any',
            },
            {
              src: '/pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'any',
            },
            {
              src: '/pwa-maskable-512x512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'maskable',
            },
          ],
          shortcuts: [
            {
              name: 'پخش سریع',
              short_name: 'پخش',
              description: 'باز کردن پخش‌کننده و ادامه پخش',
              url: '/?shortcut=play',
              icons: [{ src: '/shortcut-play.png', sizes: '192x192', type: 'image/png' }],
            },
            {
              name: 'علاقه‌مندی‌ها',
              short_name: 'علاقه‌مندی',
              description: 'مشاهده آهنگ‌های مورد علاقه',
              url: '/?shortcut=favorites',
              icons: [{ src: '/shortcut-favorites.png', sizes: '192x192', type: 'image/png' }],
            },
            {
              name: 'پخش اخیر',
              short_name: 'اخیر',
              description: 'مشاهده آخرین آهنگ‌های پخش‌شده',
              url: '/?shortcut=recent',
              icons: [{ src: '/shortcut-recent.png', sizes: '192x192', type: 'image/png' }],
            },
            {
              name: 'آلبوم‌ها',
              short_name: 'آلبوم‌ها',
              description: 'مرور موسیقی‌ها بر اساس آلبوم',
              url: '/?shortcut=albums',
              icons: [{ src: '/shortcut-albums.png', sizes: '192x192', type: 'image/png' }],
            },
          ],
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],
          runtimeCaching: [
            {
              urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
              handler: 'CacheFirst',
              options: {
                cacheName: 'google-fonts-cache',
                expiration: {
                  maxEntries: 10,
                  maxAgeSeconds: 60 * 60 * 24 * 365,
                },
                cacheableResponse: {
                  statuses: [0, 200],
                },
              },
            },
            {
              urlPattern: /^https:\/\/fonts\.gstatic\.com\/.*/i,
              handler: 'CacheFirst',
              options: {
                cacheName: 'gstatic-fonts-cache',
                expiration: {
                  maxEntries: 10,
                  maxAgeSeconds: 60 * 60 * 24 * 365,
                },
                cacheableResponse: {
                  statuses: [0, 200],
                },
              },
            },
          ],
        },
        devOptions: {
          enabled: process.env.DISABLE_HMR !== 'true',
          type: 'module',
        },
      }),
      {
        name: 'pwa-dev-manifest-no-hmr',
        apply: 'serve',
        configureServer(server) {
          if (process.env.DISABLE_HMR !== 'true') return;
          server.middlewares.use('/manifest.webmanifest', (_req, res) => {
            res.setHeader('Content-Type', 'application/manifest+json; charset=utf-8');
            res.end(
              JSON.stringify({
                id: '/',
                name: 'TO Music',
                short_name: 'TO Music',
                description:
                  'موزیک‌پلیر وب‌اپ قابل نصب روی گوشی با تم شیشه‌ای تیره، انیمیشن‌های زنده، متن هماهنگ آهنگ و ابزار زنگ گوشی',
                theme_color: '#09090b',
                background_color: '#09090b',
                display: 'standalone',
                start_url: '/',
                scope: '/',
                icons: [
                  {
                    src: '/pwa-192x192.png',
                    sizes: '192x192',
                    type: 'image/png',
                    purpose: 'any',
                  },
                  {
                    src: '/pwa-512x512.png',
                    sizes: '512x512',
                    type: 'image/png',
                    purpose: 'any',
                  },
                  {
                    src: '/pwa-maskable-512x512.png',
                    sizes: '512x512',
                    type: 'image/png',
                    purpose: 'maskable',
                  },
                ],
                shortcuts: [
                  {
                    name: 'پخش سریع',
                    short_name: 'پخش',
                    url: '/?shortcut=play',
                    icons: [{ src: '/shortcut-play.png', sizes: '192x192', type: 'image/png' }],
                  },
                  {
                    name: 'علاقه‌مندی‌ها',
                    short_name: 'علاقه‌مندی',
                    url: '/?shortcut=favorites',
                    icons: [{ src: '/shortcut-favorites.png', sizes: '192x192', type: 'image/png' }],
                  },
                  {
                    name: 'پخش اخیر',
                    short_name: 'اخیر',
                    url: '/?shortcut=recent',
                    icons: [{ src: '/shortcut-recent.png', sizes: '192x192', type: 'image/png' }],
                  },
                  {
                    name: 'آلبوم‌ها',
                    short_name: 'آلبوم‌ها',
                    url: '/?shortcut=albums',
                    icons: [{ src: '/shortcut-albums.png', sizes: '192x192', type: 'image/png' }],
                  },
                ],
              })
            );
          });
        },
        transformIndexHtml(html) {
          if (process.env.DISABLE_HMR !== 'true') return html;
          if (html.includes('rel="manifest"')) return html;
          return html.replace(
            '</head>',
            '    <link rel="manifest" href="/manifest.webmanifest" />\n  </head>'
          );
        },
      },
      // Copies src/assets into dist/ so root-relative URLs like
      // "/src/assets/images/..." (e.g. generate_image output) still resolve
      // in production builds. Do not remove.
      {
        name: 'copy-unbundled-src-assets',
        apply: 'build',
        closeBundle(error?: Error): void {
          if (error) {
            return;
          }
          const distDir = path.resolve(__dirname, 'dist');
          const srcAssets = path.resolve(__dirname, 'src/assets');
          const distAssets = path.resolve(distDir, 'src/assets');
          if (fs.existsSync(distDir) && fs.existsSync(srcAssets)) {
            fs.cpSync(srcAssets, distAssets, {recursive: true, force: true});
          }
        },
      },
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
