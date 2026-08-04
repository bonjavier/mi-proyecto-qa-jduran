Feature: yo como usuario quiero hacer login
  en la APP de SwagLabs
  y así poder realizar mis funcionalidades

  @driver:appium
  @loginCompraExitosoConCredenciales
  Scenario: login en la APP de SwagLabs
    Given el "usuario" abre la app de swagLabs
    When el usuario ingresa los datos de "standard_user" y "secret_sauce"
    And visualiza y agrega los productos al carrito
    And procede al checkout
    And ingresa sus datos de envío y finaliza la compra:
      | nombre   | apellido | codigoPostal |
      | Javier   | Duran    | 12345        |
    And confirma la compra
    Then visualiza el mensaje de confirmación "THANK YOU FOR YOU ORDER"

  @driver:appium
  @loginCredencialesInvalidas
  Scenario: login con credenciales inválidas en la APP de SwagLabs
    Given el "usuario" abre la app de swagLabs
    When el usuario ingresa los datos de "invalid_user" y "invalid_pass"
    Then visualiza el mensaje de error de login