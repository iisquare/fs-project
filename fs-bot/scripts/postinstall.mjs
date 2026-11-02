import { execSync } from 'child_process'

execSync('npx electron-builder install-app-deps', {
  stdio: 'inherit',
  env: process.env
})
