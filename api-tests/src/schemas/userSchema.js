// Contrato esperado de GET /user/me y GET /users/{id}.
export const userSchema = {
  required: ['id', 'username', 'email', 'firstName', 'lastName'],
  properties: {
    id: 'number',
    username: 'string',
    email: 'string',
    firstName: 'string',
    lastName: 'string',
    age: 'number',
    gender: 'string',
  },
};
