// Contrato esperado de POST /users/add.
// Nota: DummyJSON es un mock — simula la creación (id nuevo autoincremental,
// p.ej. ~209) pero no persiste el registro. Por eso el esquema solo valida
// la forma de la respuesta inmediata, nunca una lectura posterior por id.
export const addUserSchema = {
  required: ['id', 'firstName', 'lastName'],
  properties: {
    id: 'number',
    firstName: 'string',
    lastName: 'string',
    age: 'number',
  },
};
