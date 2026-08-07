// Contrato esperado de DELETE /users/{id}.
// DummyJSON devuelve el objeto de usuario completo más estos dos campos
// que marcan el borrado (simulado, no persiste).
export const deleteUserSchema = {
  required: ['id', 'isDeleted', 'deletedOn'],
  properties: {
    id: 'number',
    isDeleted: 'boolean',
    deletedOn: 'string',
  },
};
