import { execSync } from 'child_process'

// Ensure Python is discoverable for node-gyp
const pythonPath = 'D:/openservices/anaconda3'
process.env.PYTHON = `${pythonPath}/python.exe`
process.env.PATH = `${pythonPath};${process.env.PATH}`

execSync('npx electron-builder install-app-deps', {
  stdio: 'inherit',
  env: process.env
})
