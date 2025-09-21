// This file is loaded at runtime before Angular bundles. It can be overwritten during build/deploy.
// Example for Vercel build command:
//   echo "window.__ENV__={API_BASE_URL:'${API_BASE_URL:-}'}" > public/env.js && yarn build
(function(){
  if (typeof window === 'undefined') return;
  if (!window.__ENV__) {
    window.__ENV__ = { API_BASE_URL: '' };
  }
})();