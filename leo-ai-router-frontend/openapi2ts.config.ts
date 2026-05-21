export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: process.env.VITE_OPENAPI_SCHEMA_URL ?? 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
}
