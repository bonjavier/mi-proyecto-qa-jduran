// Contrato esperado de POST /user/login.
// Formato: { required: [...], properties: { campo: 'tipo esperado (typeof)' } }
export const loginSchema = {
  required: ['accessToken', 'refreshToken', 'id', 'username'],
  properties: {
    accessToken: 'string',
    refreshToken: 'string',
    id: 'number',
    username: 'string',
    email: 'string',
  },
};
